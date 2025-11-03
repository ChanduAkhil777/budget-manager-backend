package com.budgetmanager.backend.controller;

import com.budgetmanager.backend.model.Expense;
import com.budgetmanager.backend.model.User;
import com.budgetmanager.backend.repository.ExpenseRepository;
import com.budgetmanager.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data") 
public class DataController {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;

    public DataController(UserRepository userRepository, ExpenseRepository expenseRepository) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/budget")
    public ResponseEntity<Map<String, Double>> getBudget() {
        User user = getCurrentUser();
        return ResponseEntity.ok(Map.of("budget", user.getBudget()));
    }

    @PostMapping("/budget")
    public ResponseEntity<Map<String, Double>> setBudget(
            @RequestBody Map<String, Double> request
    ) {
        User user = getCurrentUser();
        Double newBudget = request.get("budget");
        
        if (newBudget == null || newBudget < 0) {
            return ResponseEntity.badRequest().build();
        }

        user.setBudget(newBudget);
        userRepository.save(user);
        
        return ResponseEntity.ok(Map.of("budget", user.getBudget()));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<Expense>> getExpenses() {
        User user = getCurrentUser();
        List<Expense> expenses = expenseRepository.findAllByUserId(user.getId());
        return ResponseEntity.ok(expenses);
    }

    @PostMapping("/expenses")
    public ResponseEntity<Expense> addExpense(
            @Valid @RequestBody Expense newExpense
    ) {
        User user = getCurrentUser();
        
        newExpense.setUser(user);
        Expense savedExpense = expenseRepository.save(newExpense);
        
        return ResponseEntity.ok(savedExpense);
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<?> deleteExpense(
            @PathVariable Long id
    ) {
        User user = getCurrentUser();
        
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expense.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("You do not own this expense");
        }

        expenseRepository.delete(expense);
        
        return ResponseEntity.ok(Map.of("message", "Expense deleted successfully"));
    }
}