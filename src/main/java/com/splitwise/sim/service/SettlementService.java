package com.splitwise.sim.service;

import com.splitwise.sim.dto.settlement.SettlementRequest;
import com.splitwise.sim.dto.settlement.SettlementResponse;
import com.splitwise.sim.entity.Group; // ✅ Import Group
import com.splitwise.sim.entity.Settlement;
import com.splitwise.sim.entity.User;
import com.splitwise.sim.exception.ResourceNotFoundException;
import com.splitwise.sim.repository.GroupRepository; // ✅ Import Repository
import com.splitwise.sim.repository.SettlementRepository;
import com.splitwise.sim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository; // ✅ 1. Add this
    private final LedgerService ledgerService;

    @Transactional
    public SettlementResponse settleUp(Long debtorId, SettlementRequest request) {
        User debtor = userRepository.findById(debtorId)
                .orElseThrow(() -> new ResourceNotFoundException("Debtor not found: " + debtorId));

        User creditor = userRepository.findById(request.getCreditorId())
                .orElseThrow(() -> new ResourceNotFoundException("Creditor not found: " + request.getCreditorId()));

        // ✅ 2. Fetch the Group using the ID
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + request.getGroupId()));

        if (debtorId.equals(request.getCreditorId())) {
            throw new IllegalArgumentException("Cannot settle with yourself");
        }

        // Create settlement record
        Settlement settlement = Settlement.builder()
                .debtor(debtor)
                .creditor(creditor)
                .amount(request.getAmount())
                .note(request.getNote())
                .group(group) // ✅ 3. Pass the Group object, not the ID
                .status(Settlement.SettlementStatus.COMPLETED)
                .build();

        Settlement saved = settlementRepository.save(settlement);

        // Record the REVERSE transaction
        ledgerService.recordDoubleEntry(
                creditor.getId(),
                debtor.getId(),
                request.getAmount(),
                "Settlement: " + (request.getNote() != null ? request.getNote() : "Payment received"),
                null
        );

        log.info("Settlement completed: {} paid {} to {}",
                debtor.getUsername(), request.getAmount(), creditor.getUsername());

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getUserSettlements(Long userId) {
        return settlementRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SettlementResponse> getSettlementsBetweenUsers(Long user1, Long user2) {
        return settlementRepository.findBetweenUsers(user1, user2).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private SettlementResponse mapToResponse(Settlement settlement) {
        return SettlementResponse.builder()
                .settlementId(settlement.getId())
                .debtorUsername(settlement.getDebtor().getUsername())
                .creditorUsername(settlement.getCreditor().getUsername())
                .amount(settlement.getAmount())
                .note(settlement.getNote())
                .settledAt(settlement.getSettledAt())
                .status(settlement.getStatus().name())
                .build();
    }
}