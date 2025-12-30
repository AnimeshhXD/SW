package com.splitwise.sim.controller;

import com.splitwise.sim.dto.wallet.TransactionResponse;
import com.splitwise.sim.dto.wallet.WalletBalanceResponse;
import com.splitwise.sim.service.LedgerService;
import com.splitwise.sim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
    private final LedgerService ledgerService;
    private final UserService userService;

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ledgerService.getBalance(userId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(ledgerService.getTransactionHistory(userId));
    }
}
