/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.RESTAPI_USERNAME_KEY;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.EnvoyUtil;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple;

public interface PatroniCommandUtil {

  static void reloadPatroniConfig(WebClientFactory webClientFactory, Credentials credentials) {
    final URI uri = URI.create("http://localhost:" + EnvoyUtil.PATRONI_PORT + "/reload");
    try (var webClient = webClientFactory.create(uri, Map.ofEntries(
        webClientFactory.createBasicAuthorizationHeader(
            credentials.username, credentials.password)))) {
      int status = webClient.postJson(uri, null).getStatus();
      if (status >= 300) {
        throw new RuntimeException("Can not reload patroni configuration."
            + " Endpoint /reload returned HTTP status " + status);
      }
    } catch (Exception ex) {
      throw new RuntimeException("Can not reload patroni configuration", ex);
    }
  }

  static Credentials getPatorniCredentials(
      ClusterContext context, ResourceFinder<Secret> secretFinder) {
    Secret secret = secretFinder.findByNameAndNamespace(
        context.getCluster().getMetadata().getName(),
        context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new RuntimeException("Can not find secret "
            + context.getCluster().getMetadata().getName()));
    return Optional.of(secret).map(Secret::getData)
        .filter(data -> data.get(RESTAPI_USERNAME_KEY) != null
            && data.get(RESTAPI_PASSWORD_KEY) != null)
        .map(data -> Tuple.tuple(
            data.get(RESTAPI_USERNAME_KEY),
            data.get(RESTAPI_PASSWORD_KEY))
            .map1(ResourceUtil::decodeSecret)
            .map2(ResourceUtil::decodeSecret))
        .map(t -> new Credentials(t.v1, t.v2))
        .orElseThrow(() -> new RuntimeException("Can not find key "
            + RESTAPI_PASSWORD_KEY + " and/or " + RESTAPI_USERNAME_KEY + " in secret "
            + context.getCluster().getMetadata().getName()));
  }

  record Credentials(String username, String password) {
  }

}
