/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory.CLUSTER_TEMPLATE_PATHS;

import java.util.List;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DbOpsVolumeMounts
    implements VolumeMountsProvider<StackGresDbOpsContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(StackGresDbOpsContext context) {
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
  public List<EnvVar> getDerivedEnvVars(StackGresDbOpsContext context) {
    return List.of(
        ClusterPath.TEMPLATES_PATH.envVar()
    );
  }
}
