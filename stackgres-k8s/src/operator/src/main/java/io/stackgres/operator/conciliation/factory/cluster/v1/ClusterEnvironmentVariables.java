/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.v1;

import java.util.List;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterEnvVar;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.RegistryBinding;
import io.stackgres.operator.conciliation.factory.cluster.ClusterEnvironmentVariablesFactory;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder(registry = RegistryBinding.DISABLED)
public class ClusterEnvironmentVariables
    implements ClusterEnvironmentVariablesFactory {

  @Override
  public List<EnvVar> buildEnvironmentVariables(ClusterContext context) {
    return Seq.of(ClusterPathV1.values())
        .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context))
        .append(Seq.of(ClusterEnvVar.values())
            .map(cssev -> cssev.envVar(context)))
        .collect(Collectors.toUnmodifiableList());
  }

}
