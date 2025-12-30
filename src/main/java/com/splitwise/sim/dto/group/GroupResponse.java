package com.splitwise.sim.dto.group;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data @Builder
public class GroupResponse {
    private Long id;
    private String name;
    private String description;
    private String createdByUsername;
    private Set<MemberInfo> members;
    private LocalDateTime createdAt;

    @Data @Builder
    public static class MemberInfo {
        private Long id;
        private String username;
        private String fullName;
    }
}