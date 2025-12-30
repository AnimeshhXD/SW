package com.splitwise.sim.dto.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementResponse {
    private Long settlementId;
    private String debtorUsername;
    private String creditorUsername;
    private BigDecimal amount;
    private String note;
    private LocalDateTime settledAt;
    private String status; // "COMPLETED"
}