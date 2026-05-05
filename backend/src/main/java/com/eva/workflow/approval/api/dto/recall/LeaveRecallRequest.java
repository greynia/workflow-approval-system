package com.eva.workflow.approval.api.dto.recall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaveRecallRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
