package com.accountabilityatlas.searchservice.client;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class VideoServiceClient {

  private final WebClient webClient;

  public VideoServiceClient(
      WebClient.Builder webClientBuilder,
      @Value("${app.video-service.base-url}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  public Optional<VideoDetail> getVideo(UUID videoId) {
    try {
      VideoDetail video =
          webClient
              .get()
              .uri("/videos/{id}", videoId)
              .retrieve()
              .bodyToMono(VideoDetail.class)
              .block();
      return Optional.ofNullable(video);
    } catch (WebClientResponseException.NotFound e) {
      log.warn("Video not found: {}", videoId);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Error fetching video {}: {}", videoId, e.getMessage());
      throw new RuntimeException("Failed to fetch video from video-service", e);
    }
  }
}
