/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@InitContainer(order = 0)
public class UserSetUp implements ContainerFactory<DistributedLogsContext> {

  @Override
  public Container getContainer(DistributedLogsContext context) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresComponent.KUBECTL.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
                + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(PatroniEnvPaths.getEnvVars())
        .withVolumeMounts(new VolumeMountBuilder()
                .withName("distributed-logs-templates")
                .withMountPath("/templates")
                .build(),
            new VolumeMountBuilder()
                .withName("local")
                .withMountPath("/local/etc")
                .withSubPath("etc")
                .withReadOnly(false)
                .build())
        .build();
  }

  @Override
  public List<Volume> getVolumes(DistributedLogsContext context) {
    return List.of();
  }
}
