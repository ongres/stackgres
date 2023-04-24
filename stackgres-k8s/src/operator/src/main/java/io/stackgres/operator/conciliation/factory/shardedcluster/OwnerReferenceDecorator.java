/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import java.util.List;
import java.util.Objects;

import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.factory.Decorator;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@Singleton
@OperatorVersionBinder
public class OwnerReferenceDecorator implements Decorator<StackGresShardedClusterContext> {

  @Override
  public void decorate(StackGresShardedClusterContext context,
                       Iterable<? extends HasMetadata> resources) {
    List<OwnerReference> ownerReferences = List
        .of(ResourceUtil.getControllerOwnerReference(context.getSource()));
    Seq.seq(resources)
        .filter(resource -> Objects.equals(
            resource.getMetadata().getNamespace(),
            context.getSource().getMetadata().getNamespace()))
        .filter(resource -> resource.getMetadata().getOwnerReferences().isEmpty())
        .forEach(resource -> resource.getMetadata().setOwnerReferences(ownerReferences));
  }
}
