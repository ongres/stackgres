/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.sidecars.pooling;

import static io.stackgres.operator.conciliation.StackGresRandomPasswordKeys.PGBOUNCER_ADMIN_PASSWORD_KEY;
import static io.stackgres.operator.conciliation.StackGresRandomPasswordKeys.PGBOUNCER_STATS_PASSWORD_KEY;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@InitContainer(order = 8)
public class InitPgBouncerAuthFile implements ContainerFactory<StackGresClusterContainerContext> {

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {
    return new ContainerBuilder()
        .withName("pgbouncer-auth-file")
        .withImage(StackGresComponent.KUBECTL.findLatestImageName())
        .withCommand("/bin/sh", "-exc",
            ""
                + "test -f \"/etc/pgbouncer/" + PGBOUNCER_ADMIN_PASSWORD_KEY + "\"\n"
                + "test -f \"/etc/pgbouncer/" + PGBOUNCER_STATS_PASSWORD_KEY + "\"\n"
                + "PGBOUNCER_ADMIN_MD5=\"$(printf '%spgbouncer_admin' \"$(\n"
                + "  cat \"/etc/pgbouncer/" + PGBOUNCER_ADMIN_PASSWORD_KEY + "\")\" \\\n"
                + "    | md5sum | cut -d ' ' -f 1)\"\n"
                + "PGBOUNCER_STATS_MD5=\"$(printf '%spgbouncer_stats' \"$(\n"
                + "  cat /etc/pgbouncer/" + PGBOUNCER_STATS_PASSWORD_KEY + ")\" \\\n"
                + "    | md5sum | cut -d ' ' -f 1)\"\n"
                + "(\n"
                + "echo \"\\\"pgbouncer_admin\\\" \\\"md5$PGBOUNCER_ADMIN_MD5\\\"\"\n"
                + "echo \"\\\"pgbouncer_stats\\\" \\\"md5$PGBOUNCER_STATS_MD5\\\"\"\n"
                + ") > \"" + ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.path() + "\"")
        .withImagePullPolicy("IfNotPresent")
        .addToVolumeMounts(
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_AUTH_FILE.getVolumeName())
            .withMountPath("/etc/pgbouncer")
            .withSubPath("etc/pgbouncer")
            .withReadOnly(false)
            .build(),
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_SECRETS.getVolumeName())
            .withMountPath("/etc/pgbouncer/" + PGBOUNCER_ADMIN_PASSWORD_KEY)
            .withSubPath(PGBOUNCER_ADMIN_PASSWORD_KEY)
            .withReadOnly(true)
            .build(),
            new VolumeMountBuilder()
            .withName(StatefulSetDynamicVolumes.PGBOUNCER_SECRETS.getVolumeName())
            .withMountPath("/etc/pgbouncer/" + PGBOUNCER_STATS_PASSWORD_KEY)
            .withSubPath(PGBOUNCER_STATS_PASSWORD_KEY)
            .withReadOnly(true)
            .build())
        .build();
  }

  public static void main(String[] args) {
    System.out.println(""
                + "PGBOUNCER_ADMIN_MD5=\"$(printf 'pgbouncer_admin%s' \"$(\n"
                + "  cat \"/etc/pgbouncer/" + PGBOUNCER_ADMIN_PASSWORD_KEY + "\")\" \\\n"
                + "    | md5sum | cut -d ' ' -f 1)\"\n"
                + "PGBOUNCER_STATS_MD5=\"$(printf 'pgbouncer_admin%s' \"$(\n"
                + "  cat /etc/pgbouncer/" + PGBOUNCER_STATS_PASSWORD_KEY + ")\" \\\n"
                + "    | md5sum | cut -d ' ' -f 1)\"\n"
                + "(\n"
                + "echo \"\\\"pgbouncer_admin\\\" \\\"md5$PGBOUNCER_ADMIN_MD5\\\"\"\n"
                + "echo \"\\\"pgbouncer_stats\\\" \\\"md5$PGBOUNCER_STATS_MD5\\\"\"\n"
                + ") > \"" + ClusterStatefulSetPath.PGBOUNCER_AUTH_FILE_PATH.path() + "\"");
  }
}
