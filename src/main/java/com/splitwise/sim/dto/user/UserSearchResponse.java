
package com.splitwise.sim.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private Boolean isFriend;
}