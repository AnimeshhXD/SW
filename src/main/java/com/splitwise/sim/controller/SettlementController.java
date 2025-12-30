package com.splitwise.sim.controller;

import com.splitwise.sim.dto.settlement.SettlementRequest;
import com.splitwise.sim.dto.settlement.SettlementResponse;
import com.splitwise.sim.service.SettlementService;
import com.splitwise.sim.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final UserService userService;

    /**
     * Settle up with someone you owe
     * POST /api/v1/settlements
     */
    @PostMapping
    public ResponseEntity<SettlementResponse> settleUp(
            @Valid @RequestBody SettlementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long debtorId = userService.getUserByUsername(userDetails.getUsername()).getId();
        SettlementResponse response = settlementService.settleUp(debtorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all your settlements
     * GET /api/v1/settlements
     */
    @GetMapping
    public ResponseEntity<List<SettlementResponse>> getMySettlements(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(settlementService.getUserSettlements(userId));
    }

    /**
     * Get settlements with a specific user
     * GET /api/v1/settlements/with/{userId}
     */
    @GetMapping("/with/{userId}")
    public ResponseEntity<List<SettlementResponse>> getSettlementsWithUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long myUserId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(settlementService.getSettlementsBetweenUsers(myUserId, userId));
    }
}