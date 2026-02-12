package com.accountabilityatlas.searchservice.event;

import static org.mockito.Mockito.*;

import com.accountabilityatlas.searchservice.service.IndexingService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModerationEventHandlersTest {

  @Mock private IndexingService indexingService;
  @InjectMocks private ModerationEventHandlers handlers;

  @Test
  void handleModerationEvent_videoApproved_callsIndexVideo() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoApprovedEvent event = new VideoApprovedEvent(videoId, UUID.randomUUID(), Instant.now());

    // Act
    handlers.handleModerationEvent(event);

    // Assert
    verify(indexingService).indexVideo(videoId);
  }

  @Test
  void handleModerationEvent_videoRejected_callsRemoveVideo() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoRejectedEvent event =
        new VideoRejectedEvent(videoId, UUID.randomUUID(), "OFF_TOPIC", Instant.now());

    // Act
    handlers.handleModerationEvent(event);

    // Assert
    verify(indexingService).removeVideo(videoId);
  }
}
