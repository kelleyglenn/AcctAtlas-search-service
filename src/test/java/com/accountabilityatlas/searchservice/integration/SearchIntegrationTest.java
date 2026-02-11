package com.accountabilityatlas.searchservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class SearchIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("search")
          .withUsername("search")
          .withPassword("search");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.flyway.url", postgres::getJdbcUrl);
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
  }

  @Autowired private MockMvc mockMvc;

  @Autowired private SearchVideoRepository searchVideoRepository;

  @BeforeEach
  void setUp() {
    searchVideoRepository.deleteAll();
  }

  @Test
  void search_withNoResults_returnsEmptyList() throws Exception {
    mockMvc
        .perform(get("/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results").isEmpty())
        .andExpect(jsonPath("$.pagination.totalElements").value(0));
  }

  @Test
  void search_withQuery_findsMatchingVideos() throws Exception {
    // Create test videos
    SearchVideo policeAudit = createVideo("Police Audit Downtown", "A citizen audits the police");
    SearchVideo otherVideo = createVideo("Cooking Tutorial", "How to make pasta");
    searchVideoRepository.saveAll(java.util.List.of(policeAudit, otherVideo));

    mockMvc
        .perform(get("/search").param("q", "police audit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results").isArray())
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].title").value("Police Audit Downtown"));
  }

  @Test
  void search_withAmendmentFilter_filtersResults() throws Exception {
    SearchVideo firstAmendment = createVideoWithAmendments("First Amendment Audit", "FIRST");
    SearchVideo fourthAmendment = createVideoWithAmendments("Fourth Amendment Test", "FOURTH");
    searchVideoRepository.saveAll(java.util.List.of(firstAmendment, fourthAmendment));

    mockMvc
        .perform(get("/search").param("amendments", "FIRST"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].title").value("First Amendment Audit"));
  }

  @Test
  void search_withParticipantFilter_filtersResults() throws Exception {
    SearchVideo policeVideo = createVideoWithParticipants("Police Encounter", "POLICE");
    SearchVideo governmentVideo = createVideoWithParticipants("City Hall Visit", "GOVERNMENT");
    searchVideoRepository.saveAll(java.util.List.of(policeVideo, governmentVideo));

    mockMvc
        .perform(get("/search").param("participants", "POLICE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].title").value("Police Encounter"));
  }

  @Test
  void search_withStateFilter_filtersResults() throws Exception {
    SearchVideo texasVideo = createVideoWithState("Austin Audit", "TX");
    SearchVideo californiaVideo = createVideoWithState("LA Audit", "CA");
    searchVideoRepository.saveAll(java.util.List.of(texasVideo, californiaVideo));

    mockMvc
        .perform(get("/search").param("state", "TX"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].title").value("Austin Audit"));
  }

  @Test
  void search_withPagination_returnsCorrectPage() throws Exception {
    // Create 25 videos
    for (int i = 0; i < 25; i++) {
      searchVideoRepository.save(createVideo("Video " + i, "Description " + i));
    }

    mockMvc
        .perform(get("/search").param("page", "1").param("size", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(10))
        .andExpect(jsonPath("$.pagination.page").value(1))
        .andExpect(jsonPath("$.pagination.size").value(10))
        .andExpect(jsonPath("$.pagination.totalElements").value(25))
        .andExpect(jsonPath("$.pagination.totalPages").value(3));
  }

  @Test
  void search_withMultipleFilters_combinesFilters() throws Exception {
    SearchVideo match =
        createVideo(
            "Texas Police Audit",
            "Audit in Texas",
            new String[] {"FIRST"},
            new String[] {"POLICE"},
            "TX");
    SearchVideo wrongState =
        createVideo(
            "California Police Audit",
            "Audit in CA",
            new String[] {"FIRST"},
            new String[] {"POLICE"},
            "CA");
    SearchVideo wrongAmendment =
        createVideo(
            "Texas Fourth Amendment",
            "Fourth amendment case",
            new String[] {"FOURTH"},
            new String[] {"POLICE"},
            "TX");

    searchVideoRepository.saveAll(java.util.List.of(match, wrongState, wrongAmendment));

    mockMvc
        .perform(
            get("/search")
                .param("amendments", "FIRST")
                .param("participants", "POLICE")
                .param("state", "TX"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results.length()").value(1))
        .andExpect(jsonPath("$.results[0].title").value("Texas Police Audit"));
  }

  @Test
  void search_ftsRanksRelevantResultsHigher() throws Exception {
    // Create videos where one is more relevant than the other
    SearchVideo highlyRelevant =
        createVideo(
            "Police Audit - Complete Guide",
            "This comprehensive police audit covers all aspects of auditing police");
    SearchVideo lessRelevant =
        createVideo("City Meeting", "Brief mention of police at city council meeting");

    searchVideoRepository.saveAll(java.util.List.of(highlyRelevant, lessRelevant));

    mockMvc
        .perform(get("/search").param("q", "police audit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].title").value("Police Audit - Complete Guide"));
  }

  @Test
  void actuatorHealth_isAccessible() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  private SearchVideo createVideo(String title, String description) {
    return createVideo(title, description, new String[] {}, new String[] {}, null);
  }

  private SearchVideo createVideoWithAmendments(String title, String... amendments) {
    return createVideo(title, "Description", amendments, new String[] {}, null);
  }

  private SearchVideo createVideoWithParticipants(String title, String... participants) {
    return createVideo(title, "Description", new String[] {}, participants, null);
  }

  private SearchVideo createVideoWithState(String title, String state) {
    return createVideo(title, "Description", new String[] {}, new String[] {}, state);
  }

  private SearchVideo createVideo(
      String title, String description, String[] amendments, String[] participants, String state) {
    SearchVideo video = new SearchVideo();
    video.setId(UUID.randomUUID());
    video.setYoutubeId(UUID.randomUUID().toString().substring(0, 11));
    video.setTitle(title);
    video.setDescription(description);
    video.setThumbnailUrl("https://img.youtube.com/vi/abc/default.jpg");
    video.setDurationSeconds(300);
    video.setChannelId("UC123");
    video.setChannelName("Test Channel");
    video.setVideoDate(LocalDate.now());
    video.setAmendments(amendments);
    video.setParticipants(participants);
    video.setPrimaryLocationState(state);
    video.setIndexedAt(Instant.now());
    return video;
  }
}
