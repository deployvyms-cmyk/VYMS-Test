package com.vyms.service;

import com.vyms.entity.Role;
import com.vyms.entity.User;
import com.vyms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // Creates the service with its repository and password encoder.
    @Autowired
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Returns all users from the database.
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Finds one user by id.
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Finds one user by email address.
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Saves a user and hashes the password if needed.
    public User save(User user) {
        // Hash only if the password does not look like a bcrypt hash.
        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }

    // Deletes a user by id.
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    // Checks email and password against stored credentials.
    public boolean authenticate(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.isPresent()
                && passwordEncoder.matches(password, userOpt.get().getPassword())
                && isCoreRole(userOpt.get().getRole());
    }

    private boolean isCoreRole(Role role) {
        return role == Role.ADMIN || role == Role.MANAGER;
    }

    // Returns the user only when the password matches.
    public Optional<User> getAuthenticatedUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()
                && passwordEncoder.matches(password, userOpt.get().getPassword())
                && isCoreRole(userOpt.get().getRole())) {
            return userOpt;
        }
        return Optional.empty();
    }
}
