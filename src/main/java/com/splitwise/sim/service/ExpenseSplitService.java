package com.splitwise.sim.service;

import com.splitwise.sim.dto.expense.CreateExpenseRequest;
import com.splitwise.sim.dto.expense.ExpenseParticipant;
import com.splitwise.sim.dto.expense.ExpenseResponse;
import com.splitwise.sim.entity.Expense;
import com.splitwise.sim.entity.Group;
import com.splitwise.sim.entity.User;
import com.splitwise.sim.exception.InvalidRequestException;
import com.splitwise.sim.exception.ResourceNotFoundException;
import com.splitwise.sim.repository.ExpenseRepository;
import com.splitwise.sim.repository.GroupRepository;
import com.splitwise.sim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseSplitService {

    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request, Long paidByUserId) {
        User paidBy = userRepository.findById(paidByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + paidByUserId));

        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + request.getGroupId()));

        // Validate split type
        Expense.SplitType splitType;
        try {
            splitType = Expense.SplitType.valueOf(request.getSplitType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Invalid split type: " + request.getSplitType());
        }

        // Get participant users
        Set<User> participantUsers = getParticipantUsers(request, splitType);

        // Create expense
        Expense expense = Expense.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .paidBy(paidBy)
                .group(group)
                .splitType(splitType)
                .participants(participantUsers)
                .build();

        Expense savedExpense = expenseRepository.save(expense);

        // Split based on type
        switch (splitType) {
            case EQUAL:
                splitEqually(savedExpense, request.getParticipantIds(), paidByUserId);
                break;
            case EXACT:
                splitExactly(savedExpense, request.getParticipants(), paidByUserId);
                break;
            case PERCENTAGE:
                splitByPercentage(savedExpense, request.getParticipants(), paidByUserId);
                break;
        }

        log.info("Created {} expense: {} for amount: {}",
                splitType, savedExpense.getDescription(), savedExpense.getAmount());

        return mapToResponse(savedExpense);
    }

    private Set<User> getParticipantUsers(CreateExpenseRequest request, Expense.SplitType splitType) {
        Set<Long> userIds = new HashSet<>();

        if (splitType == Expense.SplitType.EQUAL) {
            if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
                throw new InvalidRequestException("Participant IDs required for EQUAL split");
            }
            userIds.addAll(request.getParticipantIds());
        } else {
            if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
                throw new InvalidRequestException("Participants required for " + splitType + " split");
            }
            userIds.addAll(request.getParticipants().stream()
                    .map(ExpenseParticipant::getUserId)
                    .collect(Collectors.toSet()));
        }

        Set<User> users = new HashSet<>();
        for (Long userId : userIds) {
            users.add(userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Participant not found: " + userId)));
        }
        return users;
    }

    /**
     * EQUAL SPLIT: Divide equally among all participants
     */
    private void splitEqually(Expense expense, Set<Long> participantIds, Long paidByUserId) {
        BigDecimal totalAmount = expense.getAmount();
        int participantCount = participantIds.size();
        BigDecimal sharePerPerson = totalAmount.divide(
                BigDecimal.valueOf(participantCount),
                2,
                RoundingMode.HALF_UP
        );

        for (Long participantId : participantIds) {
            if (!participantId.equals(paidByUserId)) {
                ledgerService.recordDoubleEntry(
                        participantId,
                        paidByUserId,
                        sharePerPerson,
                        "Split (Equal): " + expense.getDescription(),
                        expense.getId()
                );
            }
        }
    }

    /**
     * EXACT SPLIT: Each person owes a specific amount
     */
    private void splitExactly(Expense expense, List<ExpenseParticipant> participants, Long paidByUserId) {
        BigDecimal totalAmount = expense.getAmount();
        BigDecimal sumOfSplits = BigDecimal.ZERO;

        // Validate: sum of splits should equal total amount
        for (ExpenseParticipant participant : participants) {
            if (participant.getAmount() == null || participant.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidRequestException("Invalid amount for participant: " + participant.getUserId());
            }
            sumOfSplits = sumOfSplits.add(participant.getAmount());
        }

        // Allow small rounding difference (1 cent)
        if (sumOfSplits.subtract(totalAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new InvalidRequestException(
                    String.format("Split amounts ($%s) don't match total ($%s)", sumOfSplits, totalAmount)
            );
        }

        // Record ledger entries
        for (ExpenseParticipant participant : participants) {
            if (!participant.getUserId().equals(paidByUserId)) {
                ledgerService.recordDoubleEntry(
                        participant.getUserId(),
                        paidByUserId,
                        participant.getAmount(),
                        "Split (Exact): " + expense.getDescription(),
                        expense.getId()
                );
            }
        }
    }

    /**
     * PERCENTAGE SPLIT: Each person owes a percentage of total
     */
    private void splitByPercentage(Expense expense, List<ExpenseParticipant> participants, Long paidByUserId) {
        BigDecimal totalAmount = expense.getAmount();
        BigDecimal sumOfPercentages = BigDecimal.ZERO;

        // Validate: sum of percentages should be 100
        for (ExpenseParticipant participant : participants) {
            if (participant.getPercentage() == null ||
                    participant.getPercentage().compareTo(BigDecimal.ZERO) <= 0 ||
                    participant.getPercentage().compareTo(new BigDecimal("100")) > 0) {
                throw new InvalidRequestException("Invalid percentage for participant: " + participant.getUserId());
            }
            sumOfPercentages = sumOfPercentages.add(participant.getPercentage());
        }

        if (sumOfPercentages.compareTo(new BigDecimal("100")) != 0) {
            throw new InvalidRequestException(
                    String.format("Percentages must sum to 100, got: %s", sumOfPercentages)
            );
        }

        // Calculate and record ledger entries
        for (ExpenseParticipant participant : participants) {
            BigDecimal owedAmount = totalAmount
                    .multiply(participant.getPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            if (!participant.getUserId().equals(paidByUserId)) {
                ledgerService.recordDoubleEntry(
                        participant.getUserId(),
                        paidByUserId,
                        owedAmount,
                        String.format("Split (%s%%): %s",
                                participant.getPercentage(), expense.getDescription()),
                        expense.getId()
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId) {
        return expenseRepository.findByGroupId(groupId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .paidByUsername(expense.getPaidBy().getUsername())
                .groupName(expense.getGroup() != null ? expense.getGroup().getName() : "No Group")
                .createdAt(expense.getCreatedAt())
                .build();
    }
}
