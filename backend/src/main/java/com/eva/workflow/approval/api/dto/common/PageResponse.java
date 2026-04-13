package com.eva.workflow.approval.api.dto.common;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        int currentPage,
        long totalCount,
        int pageSize,
        int totalPages
) {
}
