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
 * GKE: the cluster name is exposed directly on the GCE metadata server as an instance attribute,
 * guarded by the {@code Metadata-Flavor: Google} header. This is the reliable case.
 *
 * <p>A response is only accepted when it echoes the {@code Metadata-Flavor: Google} header (which the
 * genuine metadata server sets and a captive portal / proxy will not) and the body is a valid short
 * token.
 */
class GcpClusterNameProbe implements CloudClusterNameProbe {

  private static final Logger LOG = Logger.getLogger(GcpClusterNameProbe.class);
  private static final String ENDPOINT = "http://metadata.google.internal";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

  @Override
  public String source() {
    return "gke";
  }

  @Override
  public Optional<String> detect(HttpClient client) {
    try {
      Optional<String> name = get(client, "/computeMetadata/v1/instance/attributes/cluster-name");
      if (name.isEmpty()) {
        return Optional.empty();
      }
      Optional<String> location =
          get(client, "/computeMetadata/v1/instance/attributes/cluster-location");
      LOG.debugf("GKE cluster-name=%s cluster-location=%s", name.get(), location.orElse("?"));
      return name;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (IOException e) {
      LOG.debugf("GKE probe failed: %s", e.getMessage());
      return Optional.empty();
    }
  }

  private static Optional<String> get(HttpClient client, String path)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ENDPOINT + path))
        .timeout(REQUEST_TIMEOUT)
        .header("Metadata-Flavor", "Google")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // The genuine GCE metadata server echoes this response header; a captive portal / proxy will not.
    if (response.statusCode() != 200
        || !MetadataResponses.headerMatches(response, "Metadata-Flavor", "Google")) {
      return Optional.empty();
    }
    return MetadataResponses.validValue(response.body());
  }

}
