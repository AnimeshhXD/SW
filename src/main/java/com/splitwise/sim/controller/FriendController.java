package com.splitwise.sim.controller;

import com.splitwise.sim.entity.User;
import com.splitwise.sim.service.FriendService;
import com.splitwise.sim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;
    private final UserService userService;

    @PostMapping("/{friendId}")
    public ResponseEntity<String> addFriend(@PathVariable Long friendId,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        friendService.addFriend(userId, friendId);
        return ResponseEntity.ok("Friend added successfully");
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<String> removeFriend(@PathVariable Long friendId,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        friendService.removeFriend(userId, friendId);
        return ResponseEntity.ok("Friend removed successfully");
    }

    @GetMapping
    public ResponseEntity<Set<FriendInfo>> getFriends(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        Set<User> friends = friendService.getFriends(userId);
        Set<FriendInfo> friendInfos = friends.stream()
                .map(f -> new FriendInfo(f.getId(), f.getUsername(), f.getFullName(), f.getEmail()))
                .collect(Collectors.toSet());
        return ResponseEntity.ok(friendInfos);
    }

    public record FriendInfo(Long id, String username, String fullName, String email) {}
}