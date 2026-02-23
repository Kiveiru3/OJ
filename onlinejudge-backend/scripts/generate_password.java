// 快速生成BCrypt密码的工具
// 运行: javac -cp "path/to/spring-security-crypto.jar" generate_password.java && java -cp ".:path/to/spring-security-crypto.jar" generate_password

import java.security.MessageDigest;
import java.util.Base64;

public class generate_password {
    // 简化版BCrypt生成（仅用于测试）
    // 实际应该使用Spring Security的BCryptPasswordEncoder
    
    public static void main(String[] args) {
        System.out.println("=== Password Hash Generator ===");
        System.out.println();
        System.out.println("To generate BCrypt password, use the Java class:");
        System.out.println("  com.academic.oj.util.PasswordGenerator");
        System.out.println();
        System.out.println("Or use this SQL to reset password:");
        System.out.println();
        System.out.println("-- For password 'lirong216':");
        System.out.println("UPDATE user SET password = '$2a$10$...' WHERE username = 'Deft216';");
        System.out.println();
        System.out.println("Note: You need to run PasswordGenerator.java to get the correct hash.");
    }
}

