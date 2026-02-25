package com.accountabilityatlas.searchservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.accountabilityatlas.searchservice.repository.SearchVideoRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

  @Mock private SearchVideoRepository searchVideoRepository;

  @InjectMocks private ClusterService clusterService;

  @Test
  void findClusters_calculatesCorrectCellSizeForZoom3() {
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyString(),
            anyString()))
        .thenReturn(List.of());

    // cellSize = 180.0 / pow(2, 3) = 180.0 / 8 = 22.5
    clusterService.findClusters(24, 50, -125, -66, 3, Set.of("FIRST"), Set.of("POLICE"));

    verify(searchVideoRepository)
        .findClustersInBoundingBox(
            eq(24.0), eq(50.0), eq(-125.0), eq(-66.0), eq(22.5), anyString(), anyString());
  }

  @Test
  void findClusters_calculatesCorrectCellSizeForZoom8() {
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyString(),
            anyString()))
        .thenReturn(List.of());

    // cellSize = 180.0 / pow(2, 8) = 180.0 / 256 = 0.703125
    clusterService.findClusters(24, 50, -125, -66, 8, Set.of("FIRST"), Set.of("POLICE"));

    verify(searchVideoRepository)
        .findClustersInBoundingBox(
            eq(24.0), eq(50.0), eq(-125.0), eq(-66.0), eq(0.703125), anyString(), anyString());
  }

  @Test
  void findClusters_validatesAmendments() {
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyString(), isNull()))
        .thenReturn(List.of());

    clusterService.findClusters(24, 50, -125, -66, 5, Set.of("FIRST", "INVALID"), null);

    verify(searchVideoRepository)
        .findClustersInBoundingBox(
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            eq("{FIRST}"),
            isNull());
  }

  @Test
  void findClusters_validatesParticipants() {
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), anyString()))
        .thenReturn(List.of());

    clusterService.findClusters(24, 50, -125, -66, 5, null, Set.of("POLICE", "INVALID"));

    verify(searchVideoRepository)
        .findClustersInBoundingBox(
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            isNull(),
            eq("{POLICE}"));
  }

  @Test
  void findClusters_nullFiltersPassedAsNull() {
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), isNull()))
        .thenReturn(List.of());

    clusterService.findClusters(24, 50, -125, -66, 5, null, null);

    verify(searchVideoRepository)
        .findClustersInBoundingBox(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), isNull());
  }

  @Test
  void findClusters_convertsRawResultsToDtos() {
    Object[] row = new Object[] {37.5, -122.0, 10L, 6L, -21L, 37.0, 38.0, -123.0, -121.0};
    List<Object[]> rows = new java.util.ArrayList<>();
    rows.add(row);
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), isNull()))
        .thenReturn(rows);

    ClusterResult result = clusterService.findClusters(24, 50, -125, -66, 5, null, null);

    assertThat(result.clusters()).hasSize(1);
    ClusterResult.Cluster cluster = result.clusters().getFirst();
    assertThat(cluster.id()).isEqualTo("cluster_5_6_-21");
    assertThat(cluster.latitude()).isCloseTo(37.5, within(0.001));
    assertThat(cluster.longitude()).isCloseTo(-122.0, within(0.001));
    assertThat(cluster.count()).isEqualTo(10);
    assertThat(cluster.minLat()).isCloseTo(37.0, within(0.001));
    assertThat(cluster.maxLat()).isCloseTo(38.0, within(0.001));
    assertThat(cluster.minLng()).isCloseTo(-123.0, within(0.001));
    assertThat(cluster.maxLng()).isCloseTo(-121.0, within(0.001));
  }

  @Test
  void findClusters_calculatesTotalLocations() {
    Object[] row1 = new Object[] {37.5, -122.0, 10L, 6L, -21L, 37.0, 38.0, -123.0, -121.0};
    Object[] row2 = new Object[] {34.0, -118.0, 5L, 5L, -20L, 33.5, 34.5, -118.5, -117.5};
    List<Object[]> rows = new java.util.ArrayList<>();
    rows.add(row1);
    rows.add(row2);
    when(searchVideoRepository.findClustersInBoundingBox(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), isNull(), isNull()))
        .thenReturn(rows);

    ClusterResult result = clusterService.findClusters(24, 50, -125, -66, 5, null, null);

    assertThat(result.totalLocations()).isEqualTo(15);
    assertThat(result.zoom()).isEqualTo(5);
  }
}
