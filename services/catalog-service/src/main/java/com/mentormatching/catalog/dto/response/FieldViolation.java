package com.mentormatching.catalog.dto.response;

import lombok.Builder;

@Builder
public record FieldViolation(String field, String message) {}
