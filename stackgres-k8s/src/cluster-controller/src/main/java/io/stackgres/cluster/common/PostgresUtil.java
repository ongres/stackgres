/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.common;

import static io.stackgres.common.patroni.StackGresPasswordKeys.SUPERUSER_PASSWORD_KEY;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operatorframework.resource.ResourceUtil;

public interface PostgresUtil {

  static String getPostgresPassword(ClusterContext context, ResourceFinder<Secret> secretFinder) {
    Secret secret = secretFinder.findByNameAndNamespace(
        context.getCluster().getMetadata().getName(),
        context.getCluster().getMetadata().getNamespace())
        .orElseThrow(() -> new RuntimeException("Can not find secret "
            + context.getCluster().getMetadata().getName()));
    return Optional.of(secret).map(Secret::getData)
        .filter(data -> data.containsKey(SUPERUSER_PASSWORD_KEY))
        .map(data -> data.get(SUPERUSER_PASSWORD_KEY))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new RuntimeException("Can not find key "
            + SUPERUSER_PASSWORD_KEY + " in secret "
            + context.getCluster().getMetadata().getName()));
  }

}
