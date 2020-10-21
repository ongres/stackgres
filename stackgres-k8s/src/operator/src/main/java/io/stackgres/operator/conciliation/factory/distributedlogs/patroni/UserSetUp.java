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
@InitContainer(order = 2)
public class UserSetUp implements ContainerFactory<DistributedLogsContext> {

  @Override
  public Container getContainer(DistributedLogsContext context) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresComponent.KUBECTL.findLatestImageName())
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ecx", Seq.of(
            "USER=postgres",
            "UID=$(id -u)",
            "GID=$(id -g)",
            "SHELL=/bin/sh",
            "cp \"$TEMPLATES_PATH/passwd\" /local/etc/.",
            "cp \"$TEMPLATES_PATH/group\" /local/etc/.",
            "cp \"$TEMPLATES_PATH/shadow\" /local/etc/.",
            "cp \"$TEMPLATES_PATH/gshadow\" /local/etc/.",
            "echo \"$USER:x:$UID:$GID::$PG_BASE_PATH:$SHELL\" >> /local/etc/passwd",
            "chmod 644 /local/etc/passwd",
            "echo \"$USER:x:$GID:\" >> /local/etc/group",
            "chmod 644 /local/etc/group",
            "echo \"$USER\"':!!:18179:0:99999:7:::' >> /local/etc/shadow",
            "chmod 000 /local/etc/shadow",
            "echo \"$USER\"':!::' >> /local/etc/gshadow",
            "chmod 000 /local/etc/gshadow")
            .collect(Collectors.joining(" && ")))
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
