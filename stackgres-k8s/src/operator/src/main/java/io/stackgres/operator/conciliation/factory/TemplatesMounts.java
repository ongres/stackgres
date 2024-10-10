/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.StackGresVolume;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class TemplatesMounts implements VolumeMountsProvider<ContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ContainerContext context) {
    return List.of(
        new VolumeMountBuilder()
            .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
            .withMountPath(ClusterPathV1.TEMPLATES_PATH.path())
            .build()
    );
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ContainerContext context) {
    return Seq.of(ClusterPathV1.TEMPLATES_PATH.envVar())
        .append(AbstractTemplatesConfigMap.CLUSTER_TEMPLATE_PATHS.stream()
            .map(ClusterPathV1::envVar))
        .toList();
  }
}
