package com.accountabilityatlas.searchservice.client;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class VideoServiceClient {

  private final WebClient webClient;

  public VideoServiceClient(
      WebClient.Builder webClientBuilder, @Value("${app.video-service.base-url}") String baseUrl) {
    this.webClient = webClientBuilder.baseUrl(baseUrl).build();
  }

  /**
   * Fetches video details from video-service.
   *
   * @param videoId the video ID to fetch
   * @return the video details
   * @throws VideoNotFoundException if the video does not exist (404) - not retryable
   * @throws VideoServiceException if video-service is unavailable or returns an error - retryable
   */
  public VideoDetail getVideo(UUID videoId) {
    try {
      VideoDetail video =
          webClient
              .get()
              .uri("/videos/{id}", videoId)
              .retrieve()
              .bodyToMono(VideoDetail.class)
              .block();

      if (video == null) {
        throw new VideoNotFoundException(videoId);
      }
      return video;

    } catch (WebClientResponseException.NotFound e) {
      log.warn("Video {} not found in video-service (404)", videoId);
      throw new VideoNotFoundException(videoId);

    } catch (WebClientResponseException.ServiceUnavailable
        | WebClientResponseException.GatewayTimeout e) {
      log.error("Video-service unavailable while fetching video {}: {}", videoId, e.getMessage());
      throw new VideoServiceException(videoId, "Video-service temporarily unavailable", e, true);

    } catch (WebClientRequestException e) {
      // Connection refused, DNS failure, timeout, etc.
      log.error("Failed to connect to video-service for video {}: {}", videoId, e.getMessage());
      throw new VideoServiceException(videoId, "Failed to connect to video-service", e, true);

    } catch (WebClientResponseException e) {
      // Other HTTP errors (4xx, 5xx)
      log.error(
          "Video-service returned error {} for video {}: {}",
          e.getStatusCode(),
          videoId,
          e.getMessage());
      boolean retryable = e.getStatusCode().is5xxServerError();
      throw new VideoServiceException(
          videoId, "Video-service error: " + e.getStatusCode(), e, retryable);

    } catch (VideoNotFoundException | VideoServiceException e) {
      // Re-throw VideoNotFoundException (from null check above) without wrapping
      // Re-throw VideoServiceException without wrapping
      throw e;

    }  catch (Exception e) {
      log.error("Unexpected error fetching video {}: {}", videoId, e.getMessage());
      throw new VideoServiceException(videoId, "Unexpected error fetching video", e, true);
    }
  }
}
