package com.budgetmanager.backend.repository;

import com.budgetmanager.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // This is the correct version
    Optional<User> findByUsername(String username);
 // Inside UserRepository.java
    Optional<User> findByEmail(String email);
}