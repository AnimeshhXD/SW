package com.splitwise.sim.controller;

import com.splitwise.sim.dto.group.GroupRequest;
import com.splitwise.sim.dto.group.BalanceResponse;
import com.splitwise.sim.dto.group.GroupResponse;
import com.splitwise.sim.service.GroupService;
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
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest request,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        GroupResponse response = groupService.createGroup(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupById(groupId));
    }

    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> addMember(@PathVariable Long groupId, @PathVariable Long userId) {
        groupService.addMember(groupId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{groupId}/balance")
    public ResponseEntity<List<BalanceResponse>> getGroupBalances(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.calculateGroupBalance(groupId));
    }
}
