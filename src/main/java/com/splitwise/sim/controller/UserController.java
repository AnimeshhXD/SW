package com.splitwise.sim.controller;

import com.splitwise.sim.dto.user.UpdateProfileRequest;
import com.splitwise.sim.dto.user.UserProfileResponse;
import com.splitwise.sim.dto.user.UserSearchResponse;
import com.splitwise.sim.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Get my profile
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    /**
     * Get another user's profile
     * GET /api/v1/users/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    /**
     * Update my profile
     * PUT /api/v1/users/me
     */
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    /**
     * Search for users
     * GET /api/v1/users/search?q=john
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserByUsername(userDetails.getUsername()).getId();
        return ResponseEntity.ok(userService.searchUsers(q, userId));
    }
}
