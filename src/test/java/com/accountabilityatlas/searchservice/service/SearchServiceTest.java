package com.accountabilityatlas.searchservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.searchservice.domain.SearchVideo;
import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  @Mock private SearchVideoRepository searchVideoRepository;
  @InjectMocks private SearchService searchService;

  @Captor private ArgumentCaptor<String> queryCaptor;
  @Captor private ArgumentCaptor<String> amendmentsCaptor;
  @Captor private ArgumentCaptor<String> participantsCaptor;
  @Captor private ArgumentCaptor<String> stateCaptor;

  private SearchVideo testVideo;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    testVideo = new SearchVideo();
    testVideo.setId(UUID.randomUUID());
    testVideo.setYoutubeId("abc123");
    testVideo.setTitle("Test Video");

    pageable = PageRequest.of(0, 20);
  }

  @Test
  void search_withQueryOnly_passesQueryToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(testVideo), pageable, 1);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    SearchResult result = searchService.search("test query", null, null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(
            queryCaptor.capture(),
            amendmentsCaptor.capture(),
            participantsCaptor.capture(),
            stateCaptor.capture(),
            eq(pageable));

    assertThat(queryCaptor.getValue()).isEqualTo("test query");
    assertThat(amendmentsCaptor.getValue()).isNull();
    assertThat(participantsCaptor.getValue()).isNull();
    assertThat(stateCaptor.getValue()).isNull();
    assertThat(result.videos()).hasSize(1);
    assertThat(result.totalElements()).isEqualTo(1);
  }

  @Test
  void search_withBlankQuery_passesNullToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search("   ", null, null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(queryCaptor.capture(), any(), any(), any(), eq(pageable));

    assertThat(queryCaptor.getValue()).isNull();
  }

  @Test
  void search_withNullQuery_passesNullToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, null, null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(queryCaptor.capture(), any(), any(), any(), eq(pageable));

    assertThat(queryCaptor.getValue()).isNull();
  }

  @Test
  void search_withAmendments_convertsToPostgresArray() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, Set.of("FIRST", "FOURTH"), null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), amendmentsCaptor.capture(), any(), any(), eq(pageable));

    String amendments = amendmentsCaptor.getValue();
    assertThat(amendments).startsWith("{").contains("FIRST").contains("FOURTH").endsWith("}");
  }

  @Test
  void search_withParticipants_convertsToPostgresArray() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, null, Set.of("POLICE", "CITIZEN"), null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), any(), participantsCaptor.capture(), any(), eq(pageable));

    String participants = participantsCaptor.getValue();
    assertThat(participants).startsWith("{").contains("POLICE").contains("CITIZEN").endsWith("}");
  }

  @Test
  void search_withEmptyAmendments_passesNullToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, Set.of(), null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), amendmentsCaptor.capture(), any(), any(), eq(pageable));

    assertThat(amendmentsCaptor.getValue()).isNull();
  }

  @Test
  void search_withState_passesStateToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, null, null, "TX", pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), any(), any(), stateCaptor.capture(), eq(pageable));

    assertThat(stateCaptor.getValue()).isEqualTo("TX");
  }

  @Test
  void search_returnsCorrectPaginationInfo() {
    // Arrange
    SearchVideo video1 = new SearchVideo();
    video1.setId(UUID.randomUUID());
    SearchVideo video2 = new SearchVideo();
    video2.setId(UUID.randomUUID());

    Pageable page1 = PageRequest.of(1, 10);
    Page<SearchVideo> page = new PageImpl<>(List.of(video1, video2), page1, 25);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    SearchResult result = searchService.search("test", null, null, null, page1);

    // Assert
    assertThat(result.videos()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(25);
    assertThat(result.totalPages()).isEqualTo(3);
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(10);
  }

  @Test
  void search_recordsQueryTime() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    SearchResult result = searchService.search("test", null, null, null, pageable);

    // Assert
    assertThat(result.queryTimeMs()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void search_trimsQueryWhitespace() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search("  test query  ", null, null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(queryCaptor.capture(), any(), any(), any(), eq(pageable));

    assertThat(queryCaptor.getValue()).isEqualTo("test query");
  }

  @Test
  void search_withInvalidAmendments_filtersOutInvalidValues() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, Set.of("FIRST", "INVALID", "};DROP TABLE--"), null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), amendmentsCaptor.capture(), any(), any(), eq(pageable));

    String amendments = amendmentsCaptor.getValue();
    assertThat(amendments).isEqualTo("{FIRST}").doesNotContain("INVALID").doesNotContain("DROP");
  }

  @Test
  void search_withAllInvalidAmendments_passesNullToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, Set.of("INVALID", "ALSO_INVALID"), null, null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), amendmentsCaptor.capture(), any(), any(), eq(pageable));

    assertThat(amendmentsCaptor.getValue()).isNull();
  }

  @Test
  void search_withInvalidParticipants_filtersOutInvalidValues() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, null, Set.of("POLICE", "HACKER", "},{bad}"), null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), any(), participantsCaptor.capture(), any(), eq(pageable));

    String participants = participantsCaptor.getValue();
    assertThat(participants).isEqualTo("{POLICE}").doesNotContain("HACKER").doesNotContain("bad");
  }

  @Test
  void search_withAllInvalidParticipants_passesNullToRepository() {
    // Arrange
    Page<SearchVideo> page = new PageImpl<>(List.of(), pageable, 0);
    when(searchVideoRepository.searchWithFilters(any(), any(), any(), any(), any()))
        .thenReturn(page);

    // Act
    searchService.search(null, null, Set.of("NOT_A_PARTICIPANT"), null, pageable);

    // Assert
    verify(searchVideoRepository)
        .searchWithFilters(any(), any(), participantsCaptor.capture(), any(), eq(pageable));

    assertThat(participantsCaptor.getValue()).isNull();
  }
}
