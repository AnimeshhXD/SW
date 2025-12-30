package com.splitwise.sim.dto.group;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BalanceResponse {
    private Long userId;
    private String username;
    private Double netBalance;
}