package com.accountabilityatlas.searchservice.event;

import java.time.Instant;
import java.util.UUID;

public record VideoApprovedEvent(
    String eventType, UUID videoId, UUID reviewerId, Instant timestamp) {}
