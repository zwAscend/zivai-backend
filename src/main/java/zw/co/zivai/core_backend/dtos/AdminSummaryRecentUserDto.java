package zw.co.zivai.core_backend.dtos;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSummaryRecentUserDto {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private boolean active;
    private Set<String> roles;
    private Instant createdAt;
}
