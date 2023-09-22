/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

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
    Secret secret = secretFinder.findByNameAndNamespace(
        context.getCluster().getMetadata().getName(),
        context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new RuntimeException("Can not find secret "
            + context.getCluster().getMetadata().getName()));
    return Optional.of(secret).map(Secret::getData)
        .filter(data -> data.get(SUPERUSER_USERNAME_KEY) != null
            && data.get(SUPERUSER_PASSWORD_KEY) != null)
        .map(data -> Tuple.tuple(
            data.get(SUPERUSER_USERNAME_KEY),
            data.get(SUPERUSER_PASSWORD_KEY))
            .map1(ResourceUtil::decodeSecret)
            .map2(ResourceUtil::decodeSecret))
        .map(t -> new Credentials(t.v1, t.v2))
        .orElseThrow(() -> new RuntimeException("Can not find key "
            + SUPERUSER_PASSWORD_KEY + " and/or " + SUPERUSER_USERNAME_KEY + " in secret "
            + context.getCluster().getMetadata().getName()));
  }

  record Credentials(String username, String password) {
  }

}
