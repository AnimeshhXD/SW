package com.splitwise.sim.repository;

import com.splitwise.sim.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT SUM(CASE WHEN wt.transactionType = 'CREDIT' THEN wt.amount " +
            "ELSE -wt.amount END) FROM WalletTransaction wt WHERE wt.user.id = :userId")
    BigDecimal calculateBalance(@Param("userId") Long userId);

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.user.id = :userId " +
            "AND wt.createdAt BETWEEN :startDate AND :endDate ORDER BY wt.createdAt DESC")
    List<WalletTransaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}