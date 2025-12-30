package com.splitwise.sim.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDebtDto {
    private String debtorUsername;
    private String creditorUsername;
    private Double amount;
}