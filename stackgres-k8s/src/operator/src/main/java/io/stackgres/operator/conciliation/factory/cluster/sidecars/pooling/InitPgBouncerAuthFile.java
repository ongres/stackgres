/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_ADMIN_USERNAME;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY;
import static io.stackgres.common.patroni.StackGresPasswordKeys.PGBOUNCER_STATS_USERNAME;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.PGBOUNCER_AUTH_FILE)
public class InitPgBouncerAuthFile implements ContainerFactory<ClusterContainerContext> {

  private static final String PGBOUNCER_ADMIN_PASSWORD_PATH =
      ClusterPath.PGBOUNCER_CONFIG_PATH.path()
      + "/" + PGBOUNCER_ADMIN_PASSWORD_KEY;
  private static final String PGBOUNCER_STATS_PASSWORD_PATH =
      ClusterPath.PGBOUNCER_CONFIG_PATH.path()
      + "/" + PGBOUNCER_STATS_PASSWORD_KEY;

  @Inject
  KubectlUtil kubectl;

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return !Optional.ofNullable(context)
        .map(ClusterContainerContext::getClusterContext)
        .map(StackGresClusterContext::getSource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getDisableConnectionPooling)
        .orElse(false);
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {
    return new ContainerBuilder()
        .withName(StackGresInitContainer.PGBOUNCER_AUTH_FILE.getName())
        .withImage(kubectl.getImageName(context.getClusterContext().getCluster()))
        .withCommand("/bin/sh", "-exc",
            ""
                + "mkdir -p '" + ClusterPath.PGBOUNCER_AUTH_PATH.path() + "'\n"
                + "test -f '" + PGBOUNCER_ADMIN_PASSWORD_PATH + "'\n"
                + "test -f '" + PGBOUNCER_STATS_PASSWORD_PATH + "'\n"
                + "PGBOUNCER_ADMIN_MD5=\"$({\n"
                + "  cat '" + PGBOUNCER_ADMIN_PASSWORD_PATH + "'\n"
                + "  printf '%s' '" + PGBOUNCER_ADMIN_USERNAME + "'\n"
                + "  } | md5sum | cut -d ' ' -f 1)\"\n"
                + "PGBOUNCER_STATS_MD5=\"$({\n"
                + "  cat '" + PGBOUNCER_STATS_PASSWORD_PATH + "'\n"
                + "  printf '%s' '" + PGBOUNCER_STATS_USERNAME + "'\n"
                + "  } | md5sum | cut -d ' ' -f 1)\"\n"
                + "(\n"
                + "printf '%s\\n' \"\\\"" + PGBOUNCER_ADMIN_USERNAME + "\\\""
                    + " \\\"md5$PGBOUNCER_ADMIN_MD5\\\"\"\n"
                + "printf '%s\\n' \"\\\"" + PGBOUNCER_STATS_USERNAME + "\\\""
                    + " \\\"md5$PGBOUNCER_STATS_MD5\\\"\"\n"
                + ") > '" + ClusterPath.PGBOUNCER_AUTH_FILE_PATH.path() + "'")
        .withImagePullPolicy("IfNotPresent")
        .addToVolumeMounts(
            new VolumeMountBuilder()
            .withName(StackGresVolume.PGBOUNCER_CONFIG.getName())
            .withMountPath(ClusterPath.PGBOUNCER_CONFIG_PATH.path())
            .build(),
            new VolumeMountBuilder()
            .withName(StackGresVolume.PGBOUNCER_SECRETS.getName())
            .withMountPath(PGBOUNCER_ADMIN_PASSWORD_PATH)
            .withSubPath(PGBOUNCER_ADMIN_PASSWORD_KEY)
            .withReadOnly(true)
            .build(),
            new VolumeMountBuilder()
            .withName(StackGresVolume.PGBOUNCER_SECRETS.getName())
            .withMountPath(PGBOUNCER_STATS_PASSWORD_PATH)
            .withSubPath(PGBOUNCER_STATS_PASSWORD_KEY)
            .withReadOnly(true)
            .build())
        .build();
  }

}
