/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.v09;

import java.util.List;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.Decorator;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class OwnerReferenceDecorator implements Decorator<StackGresDistributedLogsContext> {

  @Override
  public void decorate(StackGresDistributedLogsContext context,
                       Iterable<? extends HasMetadata> resources) {
    List<OwnerReference> ownerReferences = getOwnerReferences(context.getSource());
    resources
        .forEach(resource -> {
          if (!resource.getMetadata().getOwnerReferences().isEmpty()) {
            resource.getMetadata().getOwnerReferences().stream()
                .filter(ownerReference -> ownerReference.getApiVersion().equals(
                    CommonDefinition.GROUP + "/" + CommonDefinition.VERSION))
                .forEach(ownerReference -> ownerReference.setApiVersion(
                    CommonDefinition.GROUP + "/v1beta1"));
          } else {
            resource.getMetadata().setOwnerReferences(ownerReferences);
            if (resource.getKind().equals("StatefulSet")) {
              StatefulSet sts = (StatefulSet) resource;
              sts.getSpec().getVolumeClaimTemplates()
                  .forEach(vct -> vct.getMetadata().setOwnerReferences(ownerReferences));
            }
          }
        });
  }

  private List<OwnerReference> getOwnerReferences(StackGresDistributedLogs resource) {
    return List.of(new OwnerReferenceBuilder()
        .withApiVersion(CommonDefinition.GROUP + "/v1beta1")
        .withKind(resource.getKind())
        .withName(resource.getMetadata().getName())
        .withUid(resource.getMetadata().getUid())
        .withController(true)
        .build());
  }

}
