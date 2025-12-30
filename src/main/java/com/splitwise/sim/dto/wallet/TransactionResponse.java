package com.splitwise.sim.dto.wallet;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class TransactionResponse {
    private Long id;
    private String transactionType;
    private BigDecimal amount;
    private String counterpartyUsername;
    private String description;
    private LocalDateTime createdAt;
}