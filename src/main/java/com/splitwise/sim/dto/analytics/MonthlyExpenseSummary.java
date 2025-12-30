package com.splitwise.sim.dto.analytics;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class MonthlyExpenseSummary {
    private String month;
    private BigDecimal totalSpent;
    private BigDecimal totalReceived;
    private BigDecimal netBalance;
    private Integer transactionCount;
}