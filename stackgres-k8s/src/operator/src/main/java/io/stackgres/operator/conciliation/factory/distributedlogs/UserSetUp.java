/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.SETUP_ARBITRARY_USER)
public class UserSetUp implements ContainerFactory<DistributedLogsContainerContext> {

  @Inject
  KubectlUtil kubectl;

  @Override
  public Container getContainer(DistributedLogsContainerContext context) {
    final ClusterContext clusterContext = () -> StackGresDistributedLogsUtil
        .getStackGresClusterForDistributedLogs(context.getDistributedLogsContext().getSource());

    return new ContainerBuilder()
        .withName(StackGresInitContainer.SETUP_ARBITRARY_USER.getName())
        .withImage(kubectl
            .getImageName(context.getDistributedLogsContext().getSource()))
        .withImagePullPolicy(getDefaultPullPolicy())
        .withCommand("/bin/sh", "-ex",
            ClusterPath.TEMPLATES_PATH.path()
                + "/" + ClusterPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(ClusterPath.envVars(clusterContext))
        .addToEnv(new EnvVarBuilder().withName("HOME").withValue("/tmp").build())
        .withVolumeMounts(
            new VolumeMountBuilder()
                .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
                .withMountPath(ClusterPath.TEMPLATES_PATH.path())
                .build(),
            new VolumeMountBuilder()
                .withName(StackGresVolume.USER.getName())
                .withMountPath("/local/etc")
                .withSubPath("etc")
                .build())
        .build();
  }

}
