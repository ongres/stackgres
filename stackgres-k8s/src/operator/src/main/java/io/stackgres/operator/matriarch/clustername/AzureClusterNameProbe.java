/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch.clustername;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.jboss.logging.Logger;

/**
 * AKS (best-effort): there is no direct cluster-name field in IMDS, but AKS places node VMs in a
 * managed "node resource group" named {@code MC_<resourceGroup>_<clusterName>_<location>}. We read
 * that name from IMDS and parse the cluster segment. This is a heuristic — resource-group and
 * cluster names may themselves contain underscores — so it is best-effort and only ever logged in
 * Phase 1.
 *
 * <p>Azure IMDS requires the {@code Metadata: true} request header and serves text/JSON, never HTML;
 * responses are rejected if they declare an HTML content type, and the value must both start with the
 * {@code MC_} prefix and be a valid short token, so a captive portal / random responder is rejected.
 */
class AzureClusterNameProbe implements CloudClusterNameProbe {

  private static final Logger LOG = Logger.getLogger(AzureClusterNameProbe.class);
  private static final String ENDPOINT = "http://169.254.169.254";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

  @Override
  public String source() {
    return "aks";
  }

  @Override
  public Optional<String> detect(HttpClient client) {
    try {
      Optional<String> resourceGroup = get(client,
          "/metadata/instance/compute/resourceGroupName?api-version=2021-02-01&format=text");
      Optional<String> name = resourceGroup
          .flatMap(AzureClusterNameProbe::parseClusterNameFromNodeResourceGroup)
          .flatMap(MetadataResponses::validValue);
      name.ifPresent(n -> LOG.debugf("AKS cluster name from node resource group %s: %s",
          resourceGroup.orElse("?"), n));
      return name;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (IOException e) {
      LOG.debugf("AKS probe failed: %s", e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Parse {@code MC_<resourceGroup>_<clusterName>_<location>}. Assumes the location is the last
   * segment and the cluster name the one before it (cluster names with underscores would defeat
   * this — acceptable for a best-effort, logged-only heuristic). Package-visible for unit testing.
   */
  static Optional<String> parseClusterNameFromNodeResourceGroup(String resourceGroup) {
    if (resourceGroup == null || resourceGroup.isBlank()) {
      return Optional.empty();
    }
    String rg = resourceGroup.strip();
    if (!rg.startsWith("MC_")) {
      return Optional.empty();
    }
    String[] parts = rg.split("_");
    if (parts.length < 4) {
      return Optional.empty();
    }
    String cluster = parts[parts.length - 2];
    return cluster.isBlank() ? Optional.empty() : Optional.of(cluster);
  }

  private static Optional<String> get(HttpClient client, String path)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ENDPOINT + path))
        .timeout(REQUEST_TIMEOUT)
        .header("Metadata", "true")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // Azure IMDS serves text/JSON, never HTML; reject an HTML captive-portal response.
    if (response.statusCode() != 200 || !MetadataResponses.isNotHtml(response)) {
      return Optional.empty();
    }
    return Optional.ofNullable(response.body());
  }

}
