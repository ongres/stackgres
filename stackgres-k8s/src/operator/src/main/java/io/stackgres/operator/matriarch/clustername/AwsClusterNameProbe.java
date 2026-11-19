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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

/**
 * EKS (best-effort): there is no single IMDS field for the cluster name, so this tries, in order:
 * <ol>
 *   <li>the instance tag {@code aws:eks:cluster-name} (only present when instance-metadata-tags are
 *       enabled on the node),</li>
 *   <li>parsing {@code /latest/user-data}: the AL2 {@code /etc/eks/bootstrap.sh <cluster>} argument,
 *       or the AL2023 nodeadm {@code NodeConfig} ({@code spec.cluster.name}).</li>
 * </ol>
 * Uses IMDSv2 (token) with a fall back to IMDSv1. Any miss returns empty.
 */
class AwsClusterNameProbe implements CloudClusterNameProbe {

  private static final Logger LOG = Logger.getLogger(AwsClusterNameProbe.class);
  private static final String ENDPOINT = "http://169.254.169.254";
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

  // AL2: "/etc/eks/bootstrap.sh my-cluster --kubelet-extra-args ..."
  private static final Pattern BOOTSTRAP =
      Pattern.compile("/etc/eks/bootstrap\\.sh\\s+(?:'([^']+)'|\"([^\"]+)\"|(\\S+))");
  // AL2023 nodeadm NodeConfig: spec: \n cluster: \n name: my-cluster
  private static final Pattern NODEADM_CLUSTER_NAME =
      Pattern.compile("cluster:\\s*(?:#[^\\n]*)?\\n(?:\\s+[a-zA-Z]+:[^\\n]*\\n)*?\\s+name:\\s*(\\S+)");

  @Override
  public String source() {
    return "eks";
  }

  @Override
  public Optional<String> detect(HttpClient client) {
    try {
      String token = imdsv2Token(client);
      String tag = get(client, token, "/latest/meta-data/tags/instance/aws:eks:cluster-name");
      if (tag != null && !tag.isBlank()) {
        LOG.debugf("EKS cluster name from instance tag: %s", tag.strip());
        return Optional.of(tag.strip());
      }
      String userData = get(client, token, "/latest/user-data");
      Optional<String> fromUserData = parseClusterNameFromUserData(userData);
      fromUserData.ifPresent(n -> LOG.debugf("EKS cluster name from user-data: %s", n));
      return fromUserData;
    } catch (Exception e) {
      LOG.debugf("EKS probe failed: %s", e.getMessage());
      return Optional.empty();
    }
  }

  /** Best-effort parse of an EKS node user-data blob. Package-visible for unit testing. */
  static Optional<String> parseClusterNameFromUserData(String userData) {
    if (userData == null || userData.isBlank()) {
      return Optional.empty();
    }
    Matcher bootstrap = BOOTSTRAP.matcher(userData);
    if (bootstrap.find()) {
      for (int g = 1; g <= 3; g++) {
        String v = bootstrap.group(g);
        if (v != null && !v.isBlank() && !v.startsWith("-")) {
          return Optional.of(v.strip());
        }
      }
    }
    Matcher nodeadm = NODEADM_CLUSTER_NAME.matcher(userData);
    if (nodeadm.find()) {
      String v = nodeadm.group(1);
      if (v != null && !v.isBlank()) {
        return Optional.of(v.replaceAll("[\"']", "").strip());
      }
    }
    return Optional.empty();
  }

  private static String imdsv2Token(HttpClient client) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(ENDPOINT + "/latest/api/token"))
          .timeout(REQUEST_TIMEOUT)
          .method("PUT", HttpRequest.BodyPublishers.noBody())
          .header("X-aws-ec2-metadata-token-ttl-seconds", "21600")
          .build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return response.body().strip();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (IOException e) {
      // fall back to IMDSv1 (null token)
    }
    return null;
  }

  private static String get(HttpClient client, String token, String path) throws Exception {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(ENDPOINT + path))
        .timeout(REQUEST_TIMEOUT)
        .GET();
    if (token != null) {
      builder.header("X-aws-ec2-metadata-token", token);
    }
    HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    return response.statusCode() == 200 ? response.body() : null;
  }

}
