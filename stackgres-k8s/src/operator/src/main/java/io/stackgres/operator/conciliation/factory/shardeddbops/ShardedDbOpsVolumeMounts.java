/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import static io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory.SHARDED_CLUSTER_TEMPLATE_PATHS;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ShardedClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedDbOpsVolumeMounts
    implements VolumeMountsProvider<StackGresShardedDbOpsContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresShardedDbOpsContext context) {
    return Seq.seq(SHARDED_CLUSTER_TEMPLATE_PATHS)
        .map(templatePath -> new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withMountPath(templatePath.path())
            .withSubPath(templatePath.filename())
            .withReadOnly(true)
            .build())
        .append(new VolumeMountBuilder()
            .withName(StackGresVolume.SHARED.getName())
            .withMountPath(ShardedClusterPath.SHARED_PATH.path())
            .build())
        .toList();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(StackGresShardedDbOpsContext context) {
    return List.of(
        ShardedClusterPath.TEMPLATES_PATH.envVar()
    );
  }
}
