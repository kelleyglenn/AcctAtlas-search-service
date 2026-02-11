package com.accountabilityatlas.searchservice.event;

import java.time.Instant;
import java.util.UUID;

public record VideoRejectedEvent(
    String eventType,
    UUID videoId,
    UUID reviewerId,
    String reason,
    String comment,
    Instant timestamp) {}
