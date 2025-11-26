// 临时工具：生成 BCrypt 密码哈希
// 编译运行：javac -cp "target/classes:$(mvn dependency:build-classpath -q | tail -1)" GeneratePasswordHash.java && java -cp ".:target/classes:$(mvn dependency:build-classpath -q | tail -1)" GeneratePasswordHash

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "dev123456";
        String hash = encoder.encode(password);
        System.out.println("密码: " + password);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println("");
        System.out.println("SQL 更新语句:");
        System.out.println("UPDATE user SET password = '" + hash + "' WHERE mobile = '13800000001';");
    }
}

