package com.vyms.repository;

import com.vyms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Lookup by username.
    Optional<User> findByUsername(String username);

    // Lookup by email for login and profile flows.
    Optional<User> findByEmail(String email);
}