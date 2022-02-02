/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.PGBOUNCER_ADMIN_USER_NAME;
import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresRandomPasswordKeys.PGBOUNCER_STATS_USER_NAME;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ClusterInitContainer;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V12)
@InitContainer(ClusterInitContainer.INIT_PGBOUNCER_AUTH_FILE)
public class InitPgBouncerAuthFile implements ContainerFactory<StackGresClusterContainerContext> {

  private static final String PGBOUNCER_ADMIN_PASSWORD_PATH =
      ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.path()
      + "/" + PGBOUNCER_ADMIN_PASSWORD_KEY;
  private static final String PGBOUNCER_STATS_PASSWORD_PATH =
      ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.path()
      + "/" + PGBOUNCER_STATS_PASSWORD_KEY;

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName("pgbouncer-auth-file")
        .withImage(StackGresComponent.KUBECTL.get(context.getClusterContext().getCluster())
            .findLatestImageName())
        .withCommand("/bin/sh", "-exc",
            ""
                + "test -f \"" + PGBOUNCER_ADMIN_PASSWORD_PATH + "\"\n"
                + "test -f \"" + PGBOUNCER_STATS_PASSWORD_PATH + "\"\n"
                + "PGBOUNCER_ADMIN_MD5=\"$({\n"
                + "  cat \"" + PGBOUNCER_ADMIN_PASSWORD_PATH + "\"\n"
                + "  printf '" + PGBOUNCER_ADMIN_USER_NAME + "'\n"
                + "  } | md5sum | cut -d ' ' -f 1)\"\n"
                + "PGBOUNCER_STATS_MD5=\"$({\n"
                + "  cat \"" + PGBOUNCER_STATS_PASSWORD_PATH + "\"\n"
                + "  printf '" + PGBOUNCER_STATS_USER_NAME + "'\n"
                + "  } | md5sum | cut -d ' ' -f 1)\"\n"
                + "(\n"
                + "echo \"\\\"" + PGBOUNCER_ADMIN_USER_NAME + "\\\""
                    + " \\\"md5$PGBOUNCER_ADMIN_MD5\\\"\"\n"
                + "echo \"\\\"" + PGBOUNCER_STATS_USER_NAME + "\\\""
                    + " \\\"md5$PGBOUNCER_STATS_MD5\\\"\"\n"
                + ") > \"" + ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.path() + "\"")
        .withImagePullPolicy("IfNotPresent")
        .addToVolumeMounts(
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_AUTH_FILE.getVolumeName())
            .withMountPath(ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.path())
            .withSubPath(ClusterStatefulSetPath.PGBOUNCER_AUTH_PATH.subPath())
            .build(),
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_SECRETS.getVolumeName())
            .withMountPath(PGBOUNCER_ADMIN_PASSWORD_PATH)
            .withSubPath(PGBOUNCER_ADMIN_PASSWORD_KEY)
            .withReadOnly(true)
            .build(),
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_SECRETS.getVolumeName())
            .withMountPath(PGBOUNCER_STATS_PASSWORD_PATH)
            .withSubPath(PGBOUNCER_STATS_PASSWORD_KEY)
            .withReadOnly(true)
            .build())
        .build();
  }

}
