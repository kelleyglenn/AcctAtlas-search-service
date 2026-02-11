package com.accountabilityatlas.searchservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountabilityatlas.searchservice.config.SecurityConfig;
import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.service.SearchResult;
import com.accountabilityatlas.searchservice.service.SearchService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@Import(SecurityConfig.class)
class SearchControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SearchService searchService;

  @Captor private ArgumentCaptor<Pageable> pageableCaptor;
  @Captor private ArgumentCaptor<Set<String>> amendmentsCaptor;
  @Captor private ArgumentCaptor<Set<String>> participantsCaptor;

  private SearchVideo testVideo;
  private SearchResult emptyResult;

  @BeforeEach
  void setUp() {
    testVideo = createTestVideo();
    emptyResult = new SearchResult(List.of(), 0, 0, 0, 20, 5);
  }

  @Test
  void search_returnsOkWithEmptyResults() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act & Assert
    mockMvc
        .perform(get("/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results").isEmpty())
        .andExpect(jsonPath("$.pagination.totalElements").value(0));
  }

  @Test
  void search_withQuery_passesQueryToService() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc.perform(get("/search").param("q", "police audit")).andExpect(status().isOk());

    // Assert
    verify(searchService).search(eq("police audit"), any(), any(), any(), any());
  }

  @Test
  void search_withResults_returnsVideoData() throws Exception {
    // Arrange
    SearchResult result = new SearchResult(List.of(testVideo), 1, 1, 0, 20, 10);
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(result);

    // Act & Assert
    mockMvc
        .perform(get("/search").param("q", "test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].id").value(testVideo.getId().toString()))
        .andExpect(jsonPath("$.results[0].youtubeId").value("abc123"))
        .andExpect(jsonPath("$.results[0].title").value("Test Video"))
        .andExpect(jsonPath("$.results[0].channelName").value("Test Channel"))
        .andExpect(jsonPath("$.results[0].amendments[0]").value("FIRST"))
        .andExpect(jsonPath("$.pagination.totalElements").value(1))
        .andExpect(jsonPath("$.queryTime").value(10))
        .andExpect(jsonPath("$.query").value("test"));
  }

  @Test
  void search_withAmendmentsFilter_passesAmendmentsToService() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc
        .perform(get("/search").param("amendments", "FIRST").param("amendments", "FOURTH"))
        .andExpect(status().isOk());

    // Assert
    verify(searchService).search(any(), amendmentsCaptor.capture(), any(), any(), any());
    assertThat(amendmentsCaptor.getValue()).containsExactlyInAnyOrder("FIRST", "FOURTH");
  }

  @Test
  void search_withParticipantsFilter_passesParticipantsToService() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc
        .perform(get("/search").param("participants", "POLICE").param("participants", "CITIZEN"))
        .andExpect(status().isOk());

    // Assert
    verify(searchService).search(any(), any(), participantsCaptor.capture(), any(), any());
    assertThat(participantsCaptor.getValue()).containsExactlyInAnyOrder("POLICE", "CITIZEN");
  }

  @Test
  void search_withStateFilter_passesStateToService() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc.perform(get("/search").param("state", "TX")).andExpect(status().isOk());

    // Assert
    verify(searchService).search(any(), any(), any(), eq("TX"), any());
  }

  @Test
  void search_withPagination_passesPageableToService() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc
        .perform(get("/search").param("page", "2").param("size", "50"))
        .andExpect(status().isOk());

    // Assert
    verify(searchService).search(any(), any(), any(), any(), pageableCaptor.capture());
    Pageable pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageNumber()).isEqualTo(2);
    assertThat(pageable.getPageSize()).isEqualTo(50);
  }

  @Test
  void search_withSizeOver100_capsAt100() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc.perform(get("/search").param("size", "200")).andExpect(status().isOk());

    // Assert
    verify(searchService).search(any(), any(), any(), any(), pageableCaptor.capture());
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
  }

  @Test
  void search_withDefaultPagination_usesDefaults() throws Exception {
    // Arrange
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(emptyResult);

    // Act
    mockMvc.perform(get("/search")).andExpect(status().isOk());

    // Assert
    verify(searchService).search(any(), any(), any(), any(), pageableCaptor.capture());
    Pageable pageable = pageableCaptor.getValue();
    assertThat(pageable.getPageNumber()).isEqualTo(0);
    assertThat(pageable.getPageSize()).isEqualTo(20);
  }

  @Test
  void search_withLocation_returnsLocationData() throws Exception {
    // Arrange
    SearchVideo videoWithLocation = createTestVideoWithLocation();
    SearchResult result = new SearchResult(List.of(videoWithLocation), 1, 1, 0, 20, 5);
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(result);

    // Act & Assert
    mockMvc
        .perform(get("/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].locations[0].displayName").value("City Hall"))
        .andExpect(jsonPath("$.results[0].locations[0].city").value("Austin"))
        .andExpect(jsonPath("$.results[0].locations[0].state").value("TX"))
        .andExpect(jsonPath("$.results[0].locations[0].coordinates.latitude").value(30.2672))
        .andExpect(jsonPath("$.results[0].locations[0].coordinates.longitude").value(-97.7431));
  }

  @Test
  void search_withNoLocation_returnsEmptyLocationsArray() throws Exception {
    // Arrange
    SearchResult result = new SearchResult(List.of(testVideo), 1, 1, 0, 20, 5);
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(result);

    // Act & Assert
    mockMvc
        .perform(get("/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].locations").isArray())
        .andExpect(jsonPath("$.results[0].locations").isEmpty());
  }

  @Test
  void search_withNullAmendmentsInVideo_returnsEmptySet() throws Exception {
    // Arrange
    SearchVideo videoNullArrays = new SearchVideo();
    videoNullArrays.setId(UUID.randomUUID());
    videoNullArrays.setYoutubeId("xyz789");
    videoNullArrays.setTitle("No Arrays");
    videoNullArrays.setAmendments(null);
    videoNullArrays.setParticipants(null);

    SearchResult result = new SearchResult(List.of(videoNullArrays), 1, 1, 0, 20, 5);
    when(searchService.search(any(), any(), any(), any(), any())).thenReturn(result);

    // Act & Assert
    mockMvc
        .perform(get("/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].amendments").isArray())
        .andExpect(jsonPath("$.results[0].amendments").isEmpty())
        .andExpect(jsonPath("$.results[0].participants").isArray())
        .andExpect(jsonPath("$.results[0].participants").isEmpty());
  }

  private SearchVideo createTestVideo() {
    SearchVideo video = new SearchVideo();
    video.setId(UUID.randomUUID());
    video.setYoutubeId("abc123");
    video.setTitle("Test Video");
    video.setDescription("A test video description");
    video.setThumbnailUrl("https://img.youtube.com/vi/abc123/default.jpg");
    video.setDurationSeconds(300);
    video.setChannelId("UC123");
    video.setChannelName("Test Channel");
    video.setVideoDate(LocalDate.of(2024, 1, 15));
    video.setAmendments(new String[] {"FIRST"});
    video.setParticipants(new String[] {"POLICE"});
    video.setIndexedAt(Instant.now());
    return video;
  }

  private SearchVideo createTestVideoWithLocation() {
    SearchVideo video = createTestVideo();
    video.setPrimaryLocationId(UUID.randomUUID());
    video.setPrimaryLocationName("City Hall");
    video.setPrimaryLocationCity("Austin");
    video.setPrimaryLocationState("TX");
    video.setPrimaryLocationLat(30.2672);
    video.setPrimaryLocationLng(-97.7431);
    return video;
  }
}
