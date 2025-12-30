package com.splitwise.sim.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class GroupRequest {
    @NotBlank private String name;
    private String description;
    private Set<Long> memberIds;
}