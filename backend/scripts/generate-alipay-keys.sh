#!/bin/bash
# 生成支付宝接口密钥文件脚本
# 参考：https://opendocs.alipay.com/common/02kkv7

set -e

echo "=========================================="
echo "生成支付宝接口密钥文件"
echo "=========================================="
echo ""

# 配置
KEY_DIR="./alipay-keys"
KEY_NAME="alipay_app"
KEY_SIZE=2048  # RSA密钥长度，支付宝推荐2048位

# 创建密钥目录
mkdir -p "$KEY_DIR"
cd "$KEY_DIR"

echo "📁 密钥文件将保存在: $(pwd)"
echo ""

# 检查OpenSSL
if ! command -v openssl &> /dev/null; then
    echo "❌ OpenSSL 未安装，请先安装 OpenSSL"
    echo "   Ubuntu/Debian: sudo apt-get install openssl"
    echo "   CentOS/RHEL: sudo yum install openssl"
    exit 1
fi

echo "✅ OpenSSL 已安装"
echo ""

# 1. 生成RSA私钥（PKCS#1格式）
echo "1️⃣  生成RSA私钥（PKCS#1格式）..."
openssl genrsa -out "${KEY_NAME}_private_key.pem" $KEY_SIZE
echo "   ✅ 私钥已生成: ${KEY_NAME}_private_key.pem"
echo ""

# 2. 转换为PKCS#8格式（支付宝要求的格式）
echo "2️⃣  转换为PKCS#8格式（支付宝要求）..."
openssl pkcs8 -topk8 -inform PEM -in "${KEY_NAME}_private_key.pem" \
    -outform PEM -nocrypt -out "${KEY_NAME}_private_key_pkcs8.pem"
echo "   ✅ PKCS#8私钥已生成: ${KEY_NAME}_private_key_pkcs8.pem"
echo ""

# 3. 生成公钥（X.509格式）
echo "3️⃣  生成应用公钥（X.509格式）..."
openssl rsa -in "${KEY_NAME}_private_key.pem" -pubout \
    -out "${KEY_NAME}_public_key.pem"
echo "   ✅ 公钥已生成: ${KEY_NAME}_public_key.pem"
echo ""

# 4. 生成CSR文件（证书签名请求，可选）
echo "4️⃣  生成CSR文件（证书签名请求）..."
read -p "   请输入您的组织名称（CN，如：租号酷）: " ORG_NAME
read -p "   请输入您的邮箱（可选）: " EMAIL
read -p "   请输入您的国家代码（如：CN）: " COUNTRY

# 创建CSR配置文件
cat > "${KEY_NAME}_csr.conf" <<EOF
[req]
default_bits = $KEY_SIZE
prompt = no
default_md = sha256
distinguished_name = dn

[dn]
CN=$ORG_NAME
emailAddress=${EMAIL:-admin@example.com}
C=$COUNTRY
ST=Beijing
L=Beijing
O=$ORG_NAME
OU=IT Department
EOF

openssl req -new -key "${KEY_NAME}_private_key.pem" \
    -out "${KEY_NAME}_csr.pem" \
    -config "${KEY_NAME}_csr.conf"
echo "   ✅ CSR文件已生成: ${KEY_NAME}_csr.pem"
echo ""

# 5. 提取公钥字符串（用于上传到支付宝开放平台）
echo "5️⃣  提取应用公钥字符串（用于上传到支付宝开放平台）..."
# 提取Base64内容，去除所有空白字符和非法字符
PUBLIC_KEY_STRING=$(grep -v "BEGIN\|END" "${KEY_NAME}_public_key.pem" | tr -d '\n\r\t ' | sed 's/[^A-Za-z0-9+/=]//g')

# 确保长度是4的整数倍（Base64要求）
REMAINDER=$((${#PUBLIC_KEY_STRING} % 4))
if [ $REMAINDER -ne 0 ]; then
    PADDING=$((4 - REMAINDER))
    PUBLIC_KEY_STRING="${PUBLIC_KEY_STRING}$(printf '=%.0s' $(seq 1 $PADDING))"
fi

# 验证Base64格式
if ! echo "$PUBLIC_KEY_STRING" | grep -qE '^[A-Za-z0-9+/]+={0,2}$'; then
    echo "   ⚠️  警告：公钥字符串格式可能不正确"
fi

echo "   📋 应用公钥字符串（请复制到支付宝开放平台）："
echo "   =========================================="
echo "$PUBLIC_KEY_STRING"
echo "   =========================================="
echo "   📏 字符串长度: ${#PUBLIC_KEY_STRING} (${#PUBLIC_KEY_STRING} % 4 = $((${#PUBLIC_KEY_STRING} % 4)))"
echo ""

# 6. 显示文件信息
echo "📄 生成的文件列表："
echo "   - ${KEY_NAME}_private_key.pem (PKCS#1私钥，保留备份)"
echo "   - ${KEY_NAME}_private_key_pkcs8.pem (PKCS#8私钥，用于配置)"
echo "   - ${KEY_NAME}_public_key.pem (应用公钥)"
echo "   - ${KEY_NAME}_csr.pem (证书签名请求)"
echo "   - ${KEY_NAME}_csr.conf (CSR配置文件)"
echo ""

# 7. 显示私钥内容（用于配置）
echo "🔐 PKCS#8私钥内容（用于配置到应用）："
echo "   =========================================="
cat "${KEY_NAME}_private_key_pkcs8.pem"
echo "   =========================================="
echo ""

# 8. 安全提示
echo "⚠️  安全提示："
echo "   1. 请妥善保管私钥文件，不要泄露给他人"
echo "   2. 建议将私钥文件添加到 .gitignore，不要提交到代码仓库"
echo "   3. 生产环境建议使用环境变量或密钥管理服务存储私钥"
echo "   4. 备份私钥文件到安全位置"
echo ""

# 9. 配置说明
echo "📝 配置说明："
echo "   1. 登录支付宝开放平台：https://open.alipay.com"
echo "   2. 进入应用管理 -> 选择应用 -> 接口加签方式"
echo "   3. 选择'密钥'加签方式，算法选择'RSA2'"
echo "   4. 上传上面复制的'应用公钥字符串'"
echo "   5. 在应用配置中使用'${KEY_NAME}_private_key_pkcs8.pem'文件内容"
echo ""

echo "✅ 密钥文件生成完成！"
echo ""

