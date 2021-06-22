/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.v09;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.Decorator;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class OwnerReferenceDecorator implements Decorator<StackGresCluster> {

  @Override
  public void decorate(StackGresCluster cluster,
                       Iterable<? extends HasMetadata> resources) {
    List<OwnerReference> ownerReferences = getOwnerReferences(cluster);
    resources.forEach(resource -> {
      resource.getMetadata().setOwnerReferences(ownerReferences);
      if (resource.getKind().equals("StatefulSet")) {
        StatefulSet sts = (StatefulSet) resource;
        sts.getSpec().getVolumeClaimTemplates().forEach(vct ->
            vct.getMetadata().setOwnerReferences(ownerReferences));
      }
    });
  }

  private List<OwnerReference> getOwnerReferences(StackGresCluster resource) {
    return List.of(new OwnerReferenceBuilder()
        .withApiVersion(CommonDefinition.GROUP + "/v1beta1")
        .withKind(resource.getKind())
        .withName(resource.getMetadata().getName())
        .withUid(resource.getMetadata().getUid())
        .withController(true)
        .build());
  }
}
