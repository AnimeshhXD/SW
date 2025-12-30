package com.splitwise.sim.dto.expense;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseParticipant {

    @NotNull(message = "User ID is required")
    private Long userId;

    // For EXACT split: exact amount this person owes
    private BigDecimal amount;

    // For PERCENTAGE split: percentage this person owes (0-100)
    private BigDecimal percentage;
}
