package com.splitwise.sim.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotNull(message = "Group ID is required")
    private Long groupId;

    // EQUAL | EXACT | PERCENTAGE
    @NotNull(message = "Split type is required")
    private String splitType;

    // For EQUAL split: just list of user IDs
    private Set<Long> participantIds;

    // For EXACT or PERCENTAGE split: detailed participant info
    private List<ExpenseParticipant> participants;
}