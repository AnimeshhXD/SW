package com.splitwise.sim.service;

import com.splitwise.sim.dto.group.BalanceResponse;
import com.splitwise.sim.dto.group.GroupRequest;
import com.splitwise.sim.dto.group.GroupResponse;
import com.splitwise.sim.entity.Expense;
import com.splitwise.sim.entity.Group;
import com.splitwise.sim.entity.Settlement;
import com.splitwise.sim.entity.User;
import com.splitwise.sim.exception.ResourceNotFoundException;
import com.splitwise.sim.repository.ExpenseRepository;
import com.splitwise.sim.repository.GroupRepository;
import com.splitwise.sim.repository.SettlementRepository;
import com.splitwise.sim.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    @Transactional
    public GroupResponse createGroup(GroupRequest request, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + creatorId));

        Set<User> members = new HashSet<>();
        members.add(creator);

        for (Long memberId : request.getMemberIds()) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + memberId));
            members.add(member);
        }

        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .members(members)
                .isActive(true)
                .build();

        return mapToResponse(groupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(Long userId) {
        return groupRepository.findByMemberId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        return mapToResponse(group);
    }

    @Transactional
    public void addMember(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        group.getMembers().add(user);
        groupRepository.save(group);
    }

    /**
     * Calculates the net balance for everyone in the group.
     * Returns a list of users and how much they owe (-) or are owed (+).
     */
    @Transactional(readOnly = true)
    public List<BalanceResponse> calculateGroupBalance(Long groupId) {
        // 1. Initialize map (UserId -> NetAmount)
        Map<Long, Double> balances = new HashMap<>();

        // Ensure all group members are in the map starting at 0.0
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));
        group.getMembers().forEach(member -> balances.put(member.getId(), 0.0));

        // 2. Process Expenses
        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        for (Expense expense : expenses) {
            // Convert BigDecimal to Double for calculation
            double amount = expense.getAmount().doubleValue();

            // Payer gets +Amount (They are owed this money)
            balances.merge(expense.getPaidBy().getId(), amount, Double::sum);

            // Subtract shares (Using Expense.SplitType since it is a nested enum)
            if (expense.getSplitType() == Expense.SplitType.EQUAL && !expense.getParticipants().isEmpty()) {
                double splitAmount = amount / expense.getParticipants().size();
                for (User participant : expense.getParticipants()) {
                    // Consumer gets -Amount (They owe this money)
                    balances.merge(participant.getId(), -splitAmount, Double::sum);
                }
            }
        }

        // 3. Process Settlements
        // This will now work because we updated the Entity and Repository
        List<Settlement> settlements = settlementRepository.findAllByGroupId(groupId);

        for (Settlement settlement : settlements) {
            // Explicitly convert BigDecimal to Double
            double settlementAmount = settlement.getAmount().doubleValue();

            // Debtor (Payer) balances goes UP (Debt reduces)
            balances.merge(settlement.getDebtor().getId(), settlementAmount, Double::sum);

            // Creditor (Receiver) balances goes DOWN (Owed amount reduces)
            balances.merge(settlement.getCreditor().getId(), -settlementAmount, Double::sum);
        }

        // 4. Convert to DTO List
        return balances.entrySet().stream()
                .map(entry -> {
                    User user = userRepository.findById(entry.getKey()).orElse(null);
                    String username = (user != null) ? user.getUsername() : "Unknown";
                    // Round to 2 decimal places for clean display
                    double roundedBalance = Math.round(entry.getValue() * 100.0) / 100.0;
                    return new BalanceResponse(entry.getKey(), username, roundedBalance);
                })
                .collect(Collectors.toList());
    }

    private GroupResponse mapToResponse(Group group) {
        Set<GroupResponse.MemberInfo> members = group.getMembers().stream()
                .map(m -> GroupResponse.MemberInfo.builder()
                        .id(m.getId())
                        .username(m.getUsername())
                        .fullName(m.getFullName())
                        .build())
                .collect(Collectors.toSet());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .createdByUsername(group.getCreatedBy().getUsername())
                .members(members)
                .createdAt(group.getCreatedAt())
                .build();
    }
}