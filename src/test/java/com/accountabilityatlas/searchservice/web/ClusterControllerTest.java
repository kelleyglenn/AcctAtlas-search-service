package com.accountabilityatlas.searchservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.accountabilityatlas.searchservice.config.SecurityConfig;
import com.accountabilityatlas.searchservice.service.ClusterResult;
import com.accountabilityatlas.searchservice.service.ClusterService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClusterController.class)
@Import(SecurityConfig.class)
class ClusterControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ClusterService clusterService;

  @Captor private ArgumentCaptor<Set<String>> amendmentsCaptor;
  @Captor private ArgumentCaptor<Set<String>> participantsCaptor;

  private ClusterResult emptyResult;
  private ClusterResult singleClusterResult;

  @BeforeEach
  void setUp() {
    emptyResult = new ClusterResult(List.of(), 0, 5);
    singleClusterResult =
        new ClusterResult(
            List.of(
                new ClusterResult.Cluster(
                    "cluster_5_1_-3", 37.5, -122.0, 10, 37.0, 38.0, -123.0, -121.0)),
            10,
            5);
  }

  @Test
  void cluster_withNoFilters_returnsClusters() throws Exception {
    when(clusterService.findClusters(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any(), any()))
        .thenReturn(singleClusterResult);

    mockMvc
        .perform(get("/search/cluster").param("bbox", "-125,24,-66,50").param("zoom", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clusters").isArray())
        .andExpect(jsonPath("$.clusters[0].id").value("cluster_5_1_-3"))
        .andExpect(jsonPath("$.clusters[0].coordinates.latitude").value(37.5))
        .andExpect(jsonPath("$.clusters[0].coordinates.longitude").value(-122.0))
        .andExpect(jsonPath("$.clusters[0].count").value(10))
        .andExpect(jsonPath("$.clusters[0].bounds.minLat").value(37.0))
        .andExpect(jsonPath("$.clusters[0].bounds.maxLat").value(38.0))
        .andExpect(jsonPath("$.totalLocations").value(10))
        .andExpect(jsonPath("$.zoom").value(5));
  }

  @Test
  void cluster_withAmendmentsFilter_passesToService() throws Exception {
    when(clusterService.findClusters(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any(), any()))
        .thenReturn(emptyResult);

    mockMvc
        .perform(
            get("/search/cluster")
                .param("bbox", "-125,24,-66,50")
                .param("zoom", "5")
                .param("amendments", "FIRST")
                .param("amendments", "FOURTH"))
        .andExpect(status().isOk());

    verify(clusterService)
        .findClusters(
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            amendmentsCaptor.capture(),
            any());
    assertThat(amendmentsCaptor.getValue()).containsExactlyInAnyOrder("FIRST", "FOURTH");
  }

  @Test
  void cluster_withParticipantsFilter_passesToService() throws Exception {
    when(clusterService.findClusters(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any(), any()))
        .thenReturn(emptyResult);

    mockMvc
        .perform(
            get("/search/cluster")
                .param("bbox", "-125,24,-66,50")
                .param("zoom", "5")
                .param("participants", "POLICE"))
        .andExpect(status().isOk());

    verify(clusterService)
        .findClusters(
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyDouble(),
            anyInt(),
            any(),
            participantsCaptor.capture());
    assertThat(participantsCaptor.getValue()).containsExactlyInAnyOrder("POLICE");
  }

  @Test
  void cluster_withInvalidBbox_returns400() throws Exception {
    mockMvc
        .perform(get("/search/cluster").param("bbox", "invalid").param("zoom", "5"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cluster_withWrongBboxPartCount_returns400() throws Exception {
    mockMvc
        .perform(get("/search/cluster").param("bbox", "-125,24,-66").param("zoom", "5"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cluster_missingBbox_returns400() throws Exception {
    mockMvc.perform(get("/search/cluster").param("zoom", "5")).andExpect(status().isBadRequest());
  }

  @Test
  void cluster_missingZoom_returns400() throws Exception {
    mockMvc
        .perform(get("/search/cluster").param("bbox", "-125,24,-66,50"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cluster_withZoomOutOfRange_returns400() throws Exception {
    mockMvc
        .perform(get("/search/cluster").param("bbox", "-125,24,-66,50").param("zoom", "0"))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(get("/search/cluster").param("bbox", "-125,24,-66,50").param("zoom", "21"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cluster_parsesBboxCorrectly() throws Exception {
    when(clusterService.findClusters(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any(), any()))
        .thenReturn(emptyResult);

    mockMvc
        .perform(get("/search/cluster").param("bbox", "-122.5,37.0,-121.0,38.0").param("zoom", "8"))
        .andExpect(status().isOk());

    // bbox format: minLng,minLat,maxLng,maxLat -> findClusters(minLat, maxLat, minLng, maxLng, ...)
    verify(clusterService)
        .findClusters(eq(37.0), eq(38.0), eq(-122.5), eq(-121.0), eq(8), any(), any());
  }

  @Test
  void cluster_emptyResult_returnsEmptyArray() throws Exception {
    when(clusterService.findClusters(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any(), any()))
        .thenReturn(emptyResult);

    mockMvc
        .perform(get("/search/cluster").param("bbox", "-125,24,-66,50").param("zoom", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clusters").isArray())
        .andExpect(jsonPath("$.clusters").isEmpty())
        .andExpect(jsonPath("$.totalLocations").value(0));
  }
}
