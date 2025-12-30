package com.splitwise.sim.repository;

import com.splitwise.sim.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT s FROM Settlement s WHERE s.debtor.id = :userId OR s.creditor.id = :userId " +
            "ORDER BY s.settledAt DESC")
    List<Settlement> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Settlement s WHERE " +
            "(s.debtor.id = :user1 AND s.creditor.id = :user2) OR " +
            "(s.debtor.id = :user2 AND s.creditor.id = :user1) " +
            "ORDER BY s.settledAt DESC")
    List<Settlement> findBetweenUsers(@Param("user1") Long user1, @Param("user2") Long user2);


    List<Settlement> findAllByGroupId(Long groupId);
}