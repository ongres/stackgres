/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operator.conciliation.factory.cluster.patroni.ClusterStatefulSetVolumeConfig;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;

@ApplicationScoped
public class DbOpsVolumes
    implements SubResourceStreamFactory<Volume, StackGresDbOpsContext> {

  @Override
  public Stream<Volume> streamResources(StackGresDbOpsContext config) {
    return ClusterStatefulSetVolumeConfig.allVolumes(config);
  }

}
