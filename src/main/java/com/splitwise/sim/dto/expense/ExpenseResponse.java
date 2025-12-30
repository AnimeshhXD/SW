package com.splitwise.sim.dto.expense;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class ExpenseResponse {
    private Long id;
    private String description;
    private BigDecimal amount;
    private String paidByUsername;
    private String groupName;
    private LocalDateTime createdAt;
}
