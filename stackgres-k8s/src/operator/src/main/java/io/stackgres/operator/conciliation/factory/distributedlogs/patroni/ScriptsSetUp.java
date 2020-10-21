/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.StackGresComponent;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
@InitContainer(order = 1)
public class ScriptsSetUp implements ContainerFactory<DistributedLogsContext> {

  @Override
  public Container getContainer(DistributedLogsContext context) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresComponent.KUBECTL.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ecx", Seq.of(
            "cp $TEMPLATES_PATH/start-patroni.sh \"$LOCAL_BIN_PATH\"",
            "cp $TEMPLATES_PATH/post-init.sh \"$LOCAL_BIN_PATH\"",
            "cp $TEMPLATES_PATH/exec-with-env \"$LOCAL_BIN_PATH\"",
            "sed -i \"s#\\${POSTGRES_PORT}#${POSTGRES_PORT}#g\""
                + " \"$LOCAL_BIN_PATH/post-init.sh\"",
            "sed -i \"s#\\${BASE_ENV_PATH}#${BASE_ENV_PATH}#g\""
                + " \"$LOCAL_BIN_PATH/exec-with-env\"",
            "sed -i \"s#\\${BASE_SECRET_PATH}#${BASE_SECRET_PATH}#g\""
                + " \"$LOCAL_BIN_PATH/exec-with-env\"",
            "chmod a+x \"$LOCAL_BIN_PATH/start-patroni.sh\"",
            "chmod a+x \"$LOCAL_BIN_PATH/post-init.sh\"",
            "chmod a+x \"$LOCAL_BIN_PATH/exec-with-env\"")
            .collect(Collectors.joining(" && ")))
        .withEnv(PatroniEnvPaths.getEnvVars())
        .withVolumeMounts(new VolumeMountBuilder()
                .withName("distributed-logs-templates")
                .withMountPath("/templates")
                .build(),
            new VolumeMountBuilder()
                .withName("local")
                .withMountPath("/usr/local/bin")
                .withSubPath("usr/local/bin")
                .build())
        .build();
  }

  @Override
  public List<Volume> getVolumes(DistributedLogsContext context) {
    return List.of();
  }
}
