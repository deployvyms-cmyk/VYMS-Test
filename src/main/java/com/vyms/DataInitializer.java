package com.vyms;

import com.vyms.entity.Role;
import com.vyms.entity.User;
import com.vyms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Loads default users when the application starts for the first time.
 *
 * Why this class exists:
 * - A fresh database would otherwise have no login users.
 * - These demo accounts help developers/testers access each role quickly.
 */
@Configuration
public class DataInitializer {

    /**
     * {@code @Bean} registers a startup task that runs after Spring starts.
     *
     * This task checks whether users already exist. If not, it creates one user
     * for each system role.
     */
    @Bean
    public CommandLineRunner seedUsers(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        return args -> {
            // Check admin@vyms.com
            java.util.Optional<User> adminOpt = userRepository.findByEmail("admin@vyms.com");
            if (adminOpt.isEmpty()) {
                createUser(userRepository, passwordEncoder, "Admin", "admin@vyms.com", "admin123", Role.ADMIN);
                System.out.println(">>> Admin user seeded successfully.");
            } else {
                User admin = adminOpt.get();
                if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole(Role.ADMIN);
                    userRepository.save(admin);
                    System.out.println(">>> Admin user credentials updated with correct BCrypt hash.");
                } else {
                    System.out.println(">>> Admin credentials verified successfully.");
                }
            }

            // Check manager@vyms.com
            java.util.Optional<User> managerOpt = userRepository.findByEmail("manager@vyms.com");
            if (managerOpt.isEmpty()) {
                createUser(userRepository, passwordEncoder, "Manager", "manager@vyms.com", "manager123", Role.MANAGER);
                System.out.println(">>> Manager user seeded successfully.");
            } else {
                User manager = managerOpt.get();
                if (!passwordEncoder.matches("manager123", manager.getPassword())) {
                    manager.setPassword(passwordEncoder.encode("manager123"));
                    manager.setRole(Role.MANAGER);
                    userRepository.save(manager);
                    System.out.println(">>> Manager user credentials updated with correct BCrypt hash.");
                } else {
                    System.out.println(">>> Manager credentials verified successfully.");
                }
            }
        };
    }

    /**
     * Helper method to build and save one user row.
     *
     * Important: password is encrypted with BCrypt before saving, so plain text
     * passwords are never stored in the database.
     */
    private void createUser(UserRepository repo, BCryptPasswordEncoder encoder, String username, String email,
            String password, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(encoder.encode(password));
        user.setRole(role);
        repo.save(user);
    }
}
