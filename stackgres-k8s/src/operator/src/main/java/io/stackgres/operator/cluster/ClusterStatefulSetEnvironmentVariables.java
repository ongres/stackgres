/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterStatefulSetEnvironmentVariables
    implements SubResourceStreamFactory<EnvVar, StackGresClusterContext> {

  public Stream<EnvVar> streamResources(StackGresClusterContext context) {
    return Seq.of(
        new EnvVarBuilder().withName("PG_DATA_PATH")
            .withValue(ClusterStatefulSet.PG_DATA_PATH)
            .build(),
        new EnvVarBuilder().withName("PG_RUN_PATH")
            .withValue(ClusterStatefulSet.PG_RUN_PATH)
            .build(),
        new EnvVarBuilder().withName("RESTORE_ENTRYPOINT_PATH")
            .withValue(ClusterStatefulSet.RESTORE_ENTRYPOINT_PATH)
            .build(),
        new EnvVarBuilder().withName("PATRONI_ENV")
            .withValue(ClusterStatefulSet.PATRONI_ENV)
            .build(),
        new EnvVarBuilder().withName("BACKUP_ENV")
            .withValue(ClusterStatefulSet.BACKUP_ENV)
            .build(),
        new EnvVarBuilder().withName("RESTORE_ENV")
            .withValue(ClusterStatefulSet.RESTORE_ENV)
            .build(),
        new EnvVarBuilder().withName("BASE_ENV_PATH")
            .withValue(ClusterStatefulSet.BASE_ENV_PATH)
            .build(),
        new EnvVarBuilder().withName("BASE_SECRET_PATH")
            .withValue(ClusterStatefulSet.BASE_SECRET_PATH)
            .build(),
        new EnvVarBuilder().withName("BACKUP_ENV_PATH")
            .withValue(ClusterStatefulSet.BACKUP_ENV_PATH)
            .build(),
        new EnvVarBuilder().withName("BACKUP_SECRET_PATH")
            .withValue(ClusterStatefulSet.BACKUP_SECRET_PATH)
            .build(),
        new EnvVarBuilder().withName("RESTORE_ENV_PATH")
            .withValue(ClusterStatefulSet.RESTORE_ENV_PATH)
            .build(),
        new EnvVarBuilder().withName("RESTORE_SECRET_PATH")
            .withValue(ClusterStatefulSet.RESTORE_SECRET_PATH)
            .build()
        );
  }

}
