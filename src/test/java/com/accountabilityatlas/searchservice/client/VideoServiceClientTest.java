package com.accountabilityatlas.searchservice.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
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
    VideoDetail expectedVideo = createTestVideo(videoId);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.just(expectedVideo));

    Optional<VideoDetail> result = videoServiceClient.getVideo(videoId);

    assertThat(result).isPresent();
    assertThat(result.get().id()).isEqualTo(videoId);
    assertThat(result.get().title()).isEqualTo("Test Video");
    assertThat(result.get().status()).isEqualTo("APPROVED");
  }

  @Test
  void getVideo_whenVideoNotFound_returnsEmpty() {
    WebClientResponseException notFound =
        WebClientResponseException.create(404, "Not Found", null, null, null);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(notFound));

    Optional<VideoDetail> result = videoServiceClient.getVideo(videoId);

    assertThat(result).isEmpty();
  }

  @Test
  void getVideo_whenServerError_throwsRuntimeException() {
    WebClientResponseException serverError =
        WebClientResponseException.create(500, "Internal Server Error", null, null, null);
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.error(serverError));

    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to fetch video from video-service");
  }

  @Test
  void getVideo_whenConnectionError_throwsRuntimeException() {
    when(responseSpec.bodyToMono(VideoDetail.class))
        .thenReturn(Mono.error(new RuntimeException("Connection refused")));

    assertThatThrownBy(() -> videoServiceClient.getVideo(videoId))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to fetch video from video-service");
  }

  @Test
  void getVideo_whenResponseIsNull_returnsEmpty() {
    when(responseSpec.bodyToMono(VideoDetail.class)).thenReturn(Mono.empty());

    Optional<VideoDetail> result = videoServiceClient.getVideo(videoId);

    assertThat(result).isEmpty();
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
        OffsetDateTime.now(),
        null);
  }
}
