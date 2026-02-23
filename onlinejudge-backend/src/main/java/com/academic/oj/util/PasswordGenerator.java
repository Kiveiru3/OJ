package com.academic.oj.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成和验证BCrypt密码哈希
 */
public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成密码哈希
        String[] passwords = {
            "lirong216",
            "Deft216", 
            "admin123",
            "password"
        };
        
        System.out.println("=== BCrypt Password Hash Generator ===\n");
        
        for (String password : passwords) {
            String hash = encoder.encode(password);
            System.out.println("Password: " + password);
            System.out.println("BCrypt Hash: " + hash);
            System.out.println("Hash Length: " + hash.length());
            
            // 验证
            boolean matches = encoder.matches(password, hash);
            System.out.println("Verification: " + (matches ? "✓ PASS" : "✗ FAIL"));
            System.out.println();
        }
        
        // 验证数据库中的admin密码哈希
        System.out.println("=== Verify Database Admin Hash ===\n");
        String dbHash = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy7p8f5O";
        System.out.println("Database Hash: " + dbHash);
        System.out.println("Hash Length: " + dbHash.length());
        System.out.println();
        
        String[] testPasswords = {
            "admin123",
            "admin",
            "Admin123",
            "ADMIN123"
        };
        
        boolean foundMatch = false;
        for (String password : testPasswords) {
            boolean matches = encoder.matches(password, dbHash);
            System.out.println("Password '" + password + "': " + (matches ? "✓ MATCHES" : "✗ NO MATCH"));
            if (matches) {
                foundMatch = true;
            }
        }
        
        System.out.println();
        if (!foundMatch) {
            System.out.println("⚠ WARNING: No password matches the database hash!");
            System.out.println("Generating new hash for 'admin123'...\n");
            String newHash = encoder.encode("admin123");
            System.out.println("New Hash: " + newHash);
            System.out.println("Verification: " + (encoder.matches("admin123", newHash) ? "✓ PASS" : "✗ FAIL"));
            System.out.println("\nSQL Update:");
            System.out.println("UPDATE `user` SET `password` = '" + newHash + "' WHERE `username` = 'admin';");
        } else {
            System.out.println("✓ Found matching password!");
        }
    }
}

