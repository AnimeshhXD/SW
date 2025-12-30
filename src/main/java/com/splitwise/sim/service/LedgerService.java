package com.splitwise.sim.service;

import com.splitwise.sim.dto.wallet.TransactionResponse;
import com.splitwise.sim.dto.wallet.WalletBalanceResponse;
import com.splitwise.sim.entity.User;
import com.splitwise.sim.entity.WalletTransaction;
import com.splitwise.sim.exception.ResourceNotFoundException;
import com.splitwise.sim.repository.UserRepository;
import com.splitwise.sim.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Value("${app.wallet.currency:USD}")
    private String currency;

    @Transactional
    public void recordDoubleEntry(Long fromUserId, Long toUserId, BigDecimal amount,
                                  String description, Long expenseId) {
        User fromUser = userRepository.findById(fromUserId)
                .orElseThrow(() -> new ResourceNotFoundException("From user not found: " + fromUserId));
        User toUser = userRepository.findById(toUserId)
                .orElseThrow(() -> new ResourceNotFoundException("To user not found: " + toUserId));

        String referenceId = UUID.randomUUID().toString();

        // DEBIT from payer
        WalletTransaction debit = WalletTransaction.builder()
                .user(fromUser)
                .transactionType(WalletTransaction.TransactionType.DEBIT)
                .amount(amount)
                .counterparty(toUser)
                .description(description)
                .referenceId(referenceId)
                .build();

        // CREDIT to receiver
        WalletTransaction credit = WalletTransaction.builder()
                .user(toUser)
                .transactionType(WalletTransaction.TransactionType.CREDIT)
                .amount(amount)
                .counterparty(fromUser)
                .description(description)
                .referenceId(referenceId)
                .build();

        transactionRepository.save(debit);
        transactionRepository.save(credit);
    }

    @Transactional(readOnly = true)
    public WalletBalanceResponse getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        BigDecimal balance = transactionRepository.calculateBalance(userId);
        if (balance == null) balance = BigDecimal.ZERO;

        return WalletBalanceResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .balance(balance)
                .currency(currency)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(Long userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(WalletTransaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .transactionType(txn.getTransactionType().name())
                .amount(txn.getAmount())
                .counterpartyUsername(txn.getCounterparty() != null ? txn.getCounterparty().getUsername() : "System")
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}