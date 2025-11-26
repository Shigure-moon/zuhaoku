import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 数据库中的哈希值
        String storedHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String password = "dev123456";
        
        // 验证密码
        boolean matches = encoder.matches(password, storedHash);
        System.out.println("密码验证结果: " + matches);
        
        // 如果验证失败，生成新的哈希
        if (!matches) {
            String newHash = encoder.encode(password);
            System.out.println("新密码哈希: " + newHash);
        }
    }
}

