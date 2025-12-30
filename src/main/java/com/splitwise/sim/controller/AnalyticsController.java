package com.splitwise.sim.controller;

import com.splitwise.sim.dto.analytics.GroupDebtDto;
import com.splitwise.sim.dto.analytics.MonthlyExpenseSummary;
import com.splitwise.sim.service.AnalyticsService;
import com.splitwise.sim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyExpenseSummary> getMonthlyExpenseSummary(
            @RequestParam String yearMonth,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        YearMonth ym = YearMonth.parse(yearMonth);
        return ResponseEntity.ok(analyticsService.getMonthlyExpenseSummary(userId, ym));
    }


    @GetMapping("/group/{groupId}/settlements")
    public ResponseEntity<List<GroupDebtDto>> getGroupSettlements(@PathVariable Long groupId) {
        return ResponseEntity.ok(analyticsService.calculateGroupDebts(groupId));
    }
}
