/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.REPLICATION_USERNAME_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_USERNAME_KEY;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.tuple.Tuple;

public interface PostgresUtil {

  static Credentials getPostgresCredentials(
      ClusterContext context, ResourceFinder<Secret> secretFinder) {
    return getCredentials(context, secretFinder, SUPERUSER_USERNAME_KEY, SUPERUSER_PASSWORD_KEY);
  }

  static Credentials getReplicationCredentials(
      ClusterContext context, ResourceFinder<Secret> secretFinder) {
    return getCredentials(context, secretFinder, REPLICATION_USERNAME_KEY, REPLICATION_PASSWORD_KEY);
  }

  private static Credentials getCredentials(
      ClusterContext context, ResourceFinder<Secret> secretFinder,
      String usernameKey, String passwordKey) {
    Secret secret = secretFinder.findByNameAndNamespace(
        context.getCluster().getMetadata().getName(),
        context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new RuntimeException("Can not find secret "
            + context.getCluster().getMetadata().getName()));
    return Optional.of(secret).map(Secret::getData)
        .filter(data -> data.get(usernameKey) != null
            && data.get(passwordKey) != null)
        .map(data -> Tuple.tuple(
            data.get(usernameKey),
            data.get(passwordKey))
            .map1(ResourceUtil::decodeSecret)
            .map2(ResourceUtil::decodeSecret))
        .map(t -> new Credentials(t.v1, t.v2))
        .orElseThrow(() -> new RuntimeException("Can not find key "
            + passwordKey + " and/or " + usernameKey + " in secret "
            + context.getCluster().getMetadata().getName()));
  }

  record Credentials(String username, String password) {
  }

}
