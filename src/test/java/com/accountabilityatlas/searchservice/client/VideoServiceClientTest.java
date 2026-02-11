package com.accountabilityatlas.searchservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class VideoServiceClientTest {

  @Mock private WebClient.Builder webClientBuilder;
  @Mock private WebClient webClient;
  @Mock private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
  @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;
  @Mock private WebClient.ResponseSpec responseSpec;

  private VideoServiceClient videoServiceClient;
  private UUID videoId;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    videoId = UUID.randomUUID();

    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);
    when(webClient.get()).thenReturn(requestHeadersUriSpec);
    when(requestHeadersUriSpec.uri(anyString(), any(Object.class))).thenReturn(requestHeadersSpec);
    when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

    videoServiceClient = new VideoServiceClient(webClientBuilder, "http://localhost:8082");
  }

  @Test
  void getVideo_whenVideoExists_returnsVideo() {
    // Arrange
    VideoDetail expectedVideo = createTestVideo(videoId);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.just(expectedVideo));

    // Act
    VideoDetail result = videoServiceClient.getVideo(videoId);

    // Assert
    assertThat(result.id()).isEqualTo(videoId);
    assertThat(result.title()).isEqualTo("Test Video");
    assertThat(result.status()).isEqualTo("APPROVED");
  }

  @Test
  void getVideo_whenVideoNotFound_throwsVideoNotFoundException() {
    // Arrange
    WebClientResponseException notFound =
        WebClientResponseException.create(404, "Not Found", null, null, null);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(notFound));

    // Act & Assert
    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(VideoNotFoundException.class)
        .hasMessageContaining(videoId.toString());
  }

  @Test
  void getVideo_whenServerError_throwsRetryableVideoServiceException() {
    // Arrange
    WebClientResponseException serverError =
        WebClientResponseException.create(500, "Internal Server Error", null, null, null);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(serverError));

    // Act & Assert
    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(VideoServiceException.class)
        .satisfies(
            ex -> {
              VideoServiceException vse = (VideoServiceException) ex;
              assertThat(vse.getVideoId()).isEqualTo(videoId);
              assertThat(vse.isRetryable()).isTrue();
            });
  }

  @Test
  void getVideo_whenServiceUnavailable_throwsRetryableVideoServiceException() {
    // Arrange
    WebClientResponseException unavailable =
        WebClientResponseException.create(
            HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", null, null, null);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(unavailable));

    // Act & Assert
    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(VideoServiceException.class)
        .satisfies(
            ex -> {
              VideoServiceException vse = (VideoServiceException) ex;
              assertThat(vse.isRetryable()).isTrue();
              assertThat(vse.getMessage()).contains("temporarily unavailable");
            });
  }

  @Test
  void getVideo_whenConnectionError_throwsRetryableVideoServiceException() {
    // Arrange
    WebClientRequestException connectionError =
        new WebClientRequestException(
            new RuntimeException("Connection refused"),
            HttpMethod.GET,
            URI.create("http://localhost:8082/videos/" + videoId),
            HttpHeaders.EMPTY);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(connectionError));

    // Act & Assert
    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(VideoServiceException.class)
        .satisfies(
            ex -> {
              VideoServiceException vse = (VideoServiceException) ex;
              assertThat(vse.isRetryable()).isTrue();
              assertThat(vse.getMessage()).contains("Failed to connect");
            });
  }

  @Test
  void getVideo_whenBadRequest_throwsNonRetryableVideoServiceException() {
    // Arrange
    WebClientResponseException badRequest =
        WebClientResponseException.create(400, "Bad Request", null, null, null);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(badRequest));

    // Act & Assert
    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(VideoServiceException.class)
        .satisfies(
            ex -> {
              VideoServiceException vse = (VideoServiceException) ex;
              assertThat(vse.isRetryable()).isFalse();
            });
  }

  @Test
  void getVideo_whenResponseIsNull_throwsVideoNotFoundException() {
    // Arrange
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.empty());

    // Act & Assert
    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(VideoNotFoundException.class);
  }

  private VideoDetail createTestVideo(UUID id) {
    return new VideoDetail(
        id,
        "dQw4w9WgXcQ",
        "Test Video",
        "Test description",
        "https://img.youtube.com/vi/dQw4w9WgXcQ/default.jpg",
        300,
        "UC123",
        "Test Channel",
        LocalDate.of(2024, 1, 15),
        List.of("FIRST", "FOURTH"),
        List.of("POLICE", "CITIZEN"),
        "APPROVED",
        OffsetDateTime.now(ZoneOffset.UTC),
        null);
  }
}
