/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Objects;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.Decorator;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class OwnerReferenceDecorator implements Decorator<StackGresDistributedLogsContext> {

  @Override
  public void decorate(StackGresDistributedLogsContext context,
                       Iterable<? extends HasMetadata> resources) {
    List<OwnerReference> ownerReferences = List
        .of(ResourceUtil.getOwnerReference(context.getSource()));
    Seq.seq(resources)
        .filter(resource -> Objects.equals(
            resource.getMetadata().getNamespace(),
            context.getSource().getMetadata().getNamespace()))
        .filter(resource -> resource.getMetadata().getOwnerReferences().isEmpty())
        .forEach(resource -> {
          resource.getMetadata().setOwnerReferences(ownerReferences);
          if (resource.getKind().equals("StatefulSet")) {
            StatefulSet sts = (StatefulSet) resource;
            sts.getSpec().getVolumeClaimTemplates()
                .forEach(vct -> vct.getMetadata().setOwnerReferences(ownerReferences));
          }
        });
  }
}
