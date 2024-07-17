/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap.CLUSTER_TEMPLATE_PATHS;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class StreamVolumeMounts
    implements VolumeMountsProvider<StackGresStreamContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresStreamContext context) {
    return Seq.seq(CLUSTER_TEMPLATE_PATHS)
        .map(templatePath -> new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withMountPath(templatePath.path())
            .withSubPath(templatePath.filename())
            .withReadOnly(true)
            .build())
        .append(new VolumeMountBuilder()
            .withName(StackGresVolume.SHARED.getName())
            .withMountPath(ClusterPath.SHARED_PATH.path())
            .build())
        .toList();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(StackGresStreamContext context) {
    return List.of(
        ClusterPath.TEMPLATES_PATH.envVar()
    );
  }
}
