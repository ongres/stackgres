/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch.clustername;

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
      String name = get(client, "/computeMetadata/v1/instance/attributes/cluster-name");
      if (name == null || name.isBlank()) {
        return Optional.empty();
      }
      String location = get(client, "/computeMetadata/v1/instance/attributes/cluster-location");
      LOG.debugf("GKE cluster-name=%s cluster-location=%s", name.strip(), location);
      return Optional.of(name.strip());
    } catch (Exception e) {
      LOG.debugf("GKE probe failed: %s", e.getMessage());
      return Optional.empty();
    }
  }

  private static String get(HttpClient client, String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(ENDPOINT + path))
        .timeout(REQUEST_TIMEOUT)
        .header("Metadata-Flavor", "Google")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.statusCode() == 200 ? response.body() : null;
  }

}
