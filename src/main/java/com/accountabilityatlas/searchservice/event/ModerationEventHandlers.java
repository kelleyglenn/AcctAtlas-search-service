package com.accountabilityatlas.searchservice.event;

import com.accountabilityatlas.searchservice.service.IndexingService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ModerationEventHandlers {

  private final IndexingService indexingService;

  @Bean
  public Consumer<VideoApprovedEvent> handleVideoApproved() {
    return event -> {
      log.info("Received VideoApproved event for video {}", event.videoId());
      try {
        indexingService.indexVideo(event.videoId());
      } catch (Exception e) {
        log.error("Failed to index video {}: {}", event.videoId(), e.getMessage());
        throw e; // Re-throw to trigger retry/DLQ
      }
    };
  }

  @Bean
  public Consumer<VideoRejectedEvent> handleVideoRejected() {
    return event -> {
      log.info("Received VideoRejected event for video {}", event.videoId());
      try {
        indexingService.removeVideo(event.videoId());
      } catch (Exception e) {
        log.error("Failed to remove video {}: {}", event.videoId(), e.getMessage());
        throw e;
      }
    };
  }
}
