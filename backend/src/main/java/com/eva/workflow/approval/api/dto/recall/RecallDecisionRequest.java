package com.eva.workflow.approval.api.dto.recall;

import jakarta.validation.constraints.Size;

public record RecallDecisionRequest(
        @Size(max = 1000) String comment
) {
}
