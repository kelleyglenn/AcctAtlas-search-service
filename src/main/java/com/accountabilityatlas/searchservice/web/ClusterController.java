package com.accountabilityatlas.searchservice.web;

import com.accountabilityatlas.searchservice.service.ClusterResult;
import com.accountabilityatlas.searchservice.service.ClusterService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class ClusterController {

  private final ClusterService clusterService;

  @GetMapping("/cluster")
  public ResponseEntity<Object> getClusters(
      @RequestParam String bbox,
      @RequestParam int zoom,
      @RequestParam(required = false) Set<String> amendments,
      @RequestParam(required = false) Set<String> participants) {

    if (zoom < 1 || zoom > 20) {
      return ResponseEntity.badRequest().body("Zoom must be between 1 and 20");
    }

    String[] parts = bbox.split(",");
    if (parts.length != 4) {
      return ResponseEntity.badRequest()
          .body("Invalid bbox format. Expected: minLng,minLat,maxLng,maxLat");
    }

    double minLng;
    double minLat;
    double maxLng;
    double maxLat;
    try {
      minLng = Double.parseDouble(parts[0]);
      minLat = Double.parseDouble(parts[1]);
      maxLng = Double.parseDouble(parts[2]);
      maxLat = Double.parseDouble(parts[3]);
    } catch (NumberFormatException e) {
      return ResponseEntity.badRequest()
          .body("Invalid bbox format. Expected: minLng,minLat,maxLng,maxLat");
    }

    ClusterResult result =
        clusterService.findClusters(minLat, maxLat, minLng, maxLng, zoom, amendments, participants);

    ClusterResponse response =
        new ClusterResponse(
            result.clusters().stream().map(this::toMarkerCluster).toList(),
            result.totalLocations(),
            result.zoom());

    return ResponseEntity.ok(response);
  }

  private MarkerCluster toMarkerCluster(ClusterResult.Cluster cluster) {
    return new MarkerCluster(
        cluster.id(),
        new Coordinates(cluster.latitude(), cluster.longitude()),
        cluster.count(),
        new BoundingBox(cluster.minLat(), cluster.maxLat(), cluster.minLng(), cluster.maxLng()));
  }

  // Response DTOs
  public record ClusterResponse(List<MarkerCluster> clusters, long totalLocations, int zoom) {}

  public record MarkerCluster(String id, Coordinates coordinates, long count, BoundingBox bounds) {}

  public record Coordinates(double latitude, double longitude) {}

  public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}
}
