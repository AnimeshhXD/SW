package com.splitwise.sim.service;

import com.splitwise.sim.dto.analytics.GroupDebtDto;
import com.splitwise.sim.dto.analytics.MonthlyExpenseSummary;
import com.splitwise.sim.entity.Expense;
import com.splitwise.sim.entity.User;
import com.splitwise.sim.entity.WalletTransaction;
import com.splitwise.sim.repository.ExpenseRepository;
import com.splitwise.sim.repository.UserRepository;
import com.splitwise.sim.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final WalletTransactionRepository transactionRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    /**
     * Get monthly expense summary for a user
     */
    @Transactional(readOnly = true)
    public MonthlyExpenseSummary getMonthlyExpenseSummary(Long userId, YearMonth yearMonth) {
        LocalDateTime startDate = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        List<WalletTransaction> transactions = transactionRepository.findByUserIdAndDateRange(
                userId, startDate, endDate
        );

        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal totalReceived = BigDecimal.ZERO;

        for (WalletTransaction txn : transactions) {
            if (txn.getTransactionType() == WalletTransaction.TransactionType.DEBIT) {
                totalSpent = totalSpent.add(txn.getAmount());
            } else {
                totalReceived = totalReceived.add(txn.getAmount());
            }
        }

        return MonthlyExpenseSummary.builder()
                .month(yearMonth.toString())
                .totalSpent(totalSpent)
                .totalReceived(totalReceived)
                .netBalance(totalReceived.subtract(totalSpent))
                .transactionCount(transactions.size())
                .build();
    }

    /**
     * Calculate optimal debt settlements for a group
     * Uses greedy algorithm to minimize number of transactions
     */
    @Transactional(readOnly = true)
    public List<GroupDebtDto> calculateGroupDebts(Long groupId) {
        log.info("Calculating group debts for group: {}", groupId);

        // 1. Fetch all expenses for the group
        List<Expense> groupExpenses = expenseRepository.findByGroupId(groupId);

        if (groupExpenses.isEmpty()) {
            log.info("No expenses found for group: {}", groupId);
            return Collections.emptyList();
        }

        // 2. Calculate net balances for each user
        // Positive balance = user is owed money
        // Negative balance = user owes money
        Map<Long, BigDecimal> balances = new HashMap<>();

        for (Expense expense : groupExpenses) {
            BigDecimal amount = expense.getAmount();
            Long payerId = expense.getPaidBy().getId();
            Set<User> participants = expense.getParticipants();

            // Skip if no participants
            if (participants == null || participants.isEmpty()) {
                log.warn("Expense {} has no participants, skipping", expense.getId());
                continue;
            }

            // Payer gets POSITIVE balance (they are owed)
            balances.put(payerId, balances.getOrDefault(payerId, BigDecimal.ZERO).add(amount));

            // Calculate equal split amount
            BigDecimal splitAmount = amount.divide(
                    BigDecimal.valueOf(participants.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            // Each participant gets NEGATIVE balance (they owe)
            for (User participant : participants) {
                Long participantId = participant.getId();
                balances.put(
                        participantId,
                        balances.getOrDefault(participantId, BigDecimal.ZERO).subtract(splitAmount)
                );
            }
        }

        // 3. Separate users into debtors (owe money) and creditors (are owed money)
        List<DebtorCreditor> debtors = new ArrayList<>();
        List<DebtorCreditor> creditors = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : balances.entrySet()) {
            BigDecimal balance = entry.getValue();

            // Ignore very small amounts (less than 1 cent)
            if (balance.abs().compareTo(new BigDecimal("0.01")) < 0) {
                continue;
            }

            String username = userRepository.findById(entry.getKey())
                    .map(User::getUsername)
                    .orElse("Unknown");

            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                // Negative balance = debtor (owes money)
                debtors.add(new DebtorCreditor(entry.getKey(), username, balance.abs()));
            } else {
                // Positive balance = creditor (is owed money)
                creditors.add(new DebtorCreditor(entry.getKey(), username, balance));
            }
        }

        // 4. Match debtors with creditors to minimize transactions
        List<GroupDebtDto> settlements = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < debtors.size() && j < creditors.size()) {
            DebtorCreditor debtor = debtors.get(i);
            DebtorCreditor creditor = creditors.get(j);

            // Take minimum of what debtor owes and what creditor is owed
            BigDecimal settlementAmount = debtor.amount.min(creditor.amount);

            // Create settlement
            settlements.add(GroupDebtDto.builder()
                    .debtorUsername(debtor.username)
                    .creditorUsername(creditor.username)
                    .amount(settlementAmount.doubleValue())
                    .build());

            // Update remaining amounts
            debtor.amount = debtor.amount.subtract(settlementAmount);
            creditor.amount = creditor.amount.subtract(settlementAmount);

            // Move to next debtor/creditor if settled
            if (debtor.amount.compareTo(new BigDecimal("0.01")) < 0) {
                i++;
            }
            if (creditor.amount.compareTo(new BigDecimal("0.01")) < 0) {
                j++;
            }
        }

        log.info("Calculated {} settlements for group: {}", settlements.size(), groupId);
        return settlements;
    }

    /**
     * Helper class to track debtors and creditors
     */
    private static class DebtorCreditor {
        Long userId;
        String username;
        BigDecimal amount;

        DebtorCreditor(Long userId, String username, BigDecimal amount) {
            this.userId = userId;
            this.username = username;
            this.amount = amount;
        }
    }
}
