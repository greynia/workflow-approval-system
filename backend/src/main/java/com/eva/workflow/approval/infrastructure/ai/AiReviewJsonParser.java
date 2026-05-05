package com.eva.workflow.approval.infrastructure.ai;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public final class AiReviewJsonParser {

    public static final int MAX_RISK_REASONS = 5;
    public static final int MAX_SUMMARY_LENGTH = 500;
    public static final int MAX_RISK_REASON_LENGTH = 300;
    public static final int MAX_RECOMMENDATION_REASON_LENGTH = 500;

    private AiReviewJsonParser() {}

    public static String requiredText(JsonNode result, String fieldName, int maxLength) {
        String value = result.path(fieldName).asText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return truncate(value.trim(), maxLength);
    }

    public static <T extends Enum<T>> T parseEnum(JsonNode result, String fieldName, Class<T> enumClass) {
        String value = result.path(fieldName).asText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }
        return Enum.valueOf(enumClass, value.trim());
    }

    public static List<String> parseRiskReasons(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("riskReasons must be an array");
        }
        List<String> reasons = new ArrayList<>();
        for (JsonNode item : node) {
            if (reasons.size() >= MAX_RISK_REASONS) break;
            if (!item.isTextual()) {
                throw new IllegalArgumentException("riskReasons must contain only strings");
            }
            String reason = item.asText().trim();
            if (!reason.isBlank()) {
                reasons.add(truncate(reason, MAX_RISK_REASON_LENGTH));
            }
        }
        return List.copyOf(reasons);
    }

    public static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
