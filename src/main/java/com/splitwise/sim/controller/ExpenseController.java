package com.splitwise.sim.controller;

import com.splitwise.sim.dto.expense.CreateExpenseRequest;
import com.splitwise.sim.dto.expense.ExpenseResponse;
import com.splitwise.sim.service.ExpenseSplitService;
import com.splitwise.sim.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseSplitService expenseSplitService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody CreateExpenseRequest request,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        ExpenseResponse response = expenseSplitService.createExpense(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<ExpenseResponse>> getGroupExpenses(@PathVariable Long groupId) {
        return ResponseEntity.ok(expenseSplitService.getGroupExpenses(groupId));
    }
}
