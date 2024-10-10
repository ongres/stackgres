/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.ClusterEnvVar;
import io.stackgres.common.ClusterPathV1;
import io.stackgres.common.ClusterPathV2;
import io.stackgres.common.StackGresUtil;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DbOpsEnvironmentVariables
    implements SubResourceStreamFactory<EnvVar, StackGresDbOpsContext> {

  public Stream<EnvVar> streamResources(StackGresDbOpsContext context) {
    return (StackGresUtil.isRegistryEnabled(context.getCluster())
        ? Seq.of(ClusterPathV2.values())
            .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context))
        : Seq.of(ClusterPathV1.values())
            .map(clusterStatefulSetPath -> clusterStatefulSetPath.envVar(context)))
        .append(Seq.of(ClusterEnvVar.values())
            .map(clusterStatefulEnvVar -> clusterStatefulEnvVar.envVar(context)));
  }

}
