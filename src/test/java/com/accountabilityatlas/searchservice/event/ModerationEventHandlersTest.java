package com.accountabilityatlas.searchservice.event;

import static org.mockito.Mockito.*;

import com.accountabilityatlas.searchservice.service.IndexingService;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
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
  void handleVideoApproved_callsIndexVideo() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoApprovedEvent event =
        new VideoApprovedEvent("VideoApproved", videoId, UUID.randomUUID(), Instant.now());

    // Act
    Consumer<VideoApprovedEvent> handler = handlers.handleVideoApproved();
    handler.accept(event);

    // Assert
    verify(indexingService).indexVideo(videoId);
  }

  @Test
  void handleVideoRejected_callsRemoveVideo() {
    // Arrange
    UUID videoId = UUID.randomUUID();
    VideoRejectedEvent event =
        new VideoRejectedEvent(
            "VideoRejected", videoId, UUID.randomUUID(), "OFF_TOPIC", null, Instant.now());

    // Act
    Consumer<VideoRejectedEvent> handler = handlers.handleVideoRejected();
    handler.accept(event);

    // Assert
    verify(indexingService).removeVideo(videoId);
  }
}
