-- 重置用户密码脚本
-- 用于重新生成BCrypt密码哈希

-- 方法1：使用已知的BCrypt哈希（对应密码 "lirong216"）
-- 注意：这个哈希值需要先用Java代码生成
UPDATE `user` 
SET `password` = '$2a$10$BXtR0M65r7HAh86HeYt/h.ODrg3q2XMCyB9fdchGawkEkvp6SgZzK'
WHERE `username` = 'Deft216';

-- 方法2：如果上面的密码不对，需要重新生成
-- 使用以下Java代码生成新的BCrypt哈希：
/*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "your_password_here";  // 替换为实际密码
        String hash = encoder.encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        
        // 验证
        boolean matches = encoder.matches(password, hash);
        System.out.println("Matches: " + matches);
    }
}
*/

-- 验证密码长度
SELECT 
    username, 
    LENGTH(password) as password_length,
    LEFT(password, 10) as password_prefix,
    password
FROM `user`
WHERE `username` = 'Deft216';

