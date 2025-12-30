package com.splitwise.sim.dto.settlement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRequest {

    @NotNull(message = "Creditor ID is required")
    private Long creditorId; // Person who is OWED money


    @NotNull(message = "Group ID is required")
    private Long groupId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String note; // Optional note like "Cash payment" or "Bank transfer"
}