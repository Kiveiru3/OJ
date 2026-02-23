import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 测试admin密码匹配
 */
public class TestAdminPassword {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 数据库中的密码哈希
        String dbHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O";
        
        // 测试密码
        String[] testPasswords = {
            "admin123",
            "admin",
            "Admin123",
            "ADMIN123",
            "admin1234"
        };
        
        System.out.println("=== 测试Admin密码匹配 ===\n");
        System.out.println("数据库中的哈希: " + dbHash);
        System.out.println("哈希长度: " + dbHash.length());
        System.out.println();
        
        boolean found = false;
        for (String password : testPasswords) {
            boolean matches = encoder.matches(password, dbHash);
            System.out.println("密码 '" + password + "': " + (matches ? "✓ 匹配" : "✗ 不匹配"));
            if (matches) {
                found = true;
            }
        }
        
        System.out.println();
        if (!found) {
            System.out.println("⚠ 警告: 没有找到匹配的密码！");
            System.out.println("正在生成新的密码哈希...\n");
            
            // 生成新的admin123密码哈希
            String newHash = encoder.encode("admin123");
            System.out.println("新密码: admin123");
            System.out.println("新哈希: " + newHash);
            System.out.println("哈希长度: " + newHash.length());
            
            // 验证新哈希
            boolean verify = encoder.matches("admin123", newHash);
            System.out.println("验证结果: " + (verify ? "✓ 通过" : "✗ 失败"));
            
            System.out.println("\n=== SQL更新语句 ===");
            System.out.println("UPDATE `user` SET `password` = '" + newHash + "' WHERE `username` = 'admin';");
        } else {
            System.out.println("✓ 找到匹配的密码！");
        }
    }
}

