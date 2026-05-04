package com.eva.workflow.approval.api.dto.admin;

public record HolidayImportResultResponse(
        int imported,
        int skipped
) {
}
