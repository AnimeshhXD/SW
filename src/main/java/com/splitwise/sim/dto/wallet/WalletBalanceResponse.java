package com.splitwise.sim.dto.wallet;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class WalletBalanceResponse {
    private Long userId;
    private String username;
    private BigDecimal balance;
    private String currency;
}