/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.Decorator;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
public class OwnerReferenceDecoratorImpl implements
    Decorator<StackGresCluster> {

  @Override
  public void decorate(StackGresCluster cluster,
                       Iterable<? extends HasMetadata> resources) {

    List<OwnerReference> ownerReferences = List
        .of(ResourceUtil.getOwnerReference(cluster));
    resources.forEach(resource -> {
      resource.getMetadata().setOwnerReferences(ownerReferences);
      if (resource.getKind().equals("StatefulSet")) {
        StatefulSet sts = (StatefulSet) resource;
        sts.getSpec().getVolumeClaimTemplates()
            .forEach(vct -> vct.getMetadata().setOwnerReferences(ownerReferences));
      }
    });
  }
}
