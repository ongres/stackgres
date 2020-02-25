/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.common.StackgresClusterReview;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operatorframework.admissionwebhook.Operation;

public abstract class AbstractDefaultResourceMutator<R extends CustomResource>
    implements ClusterMutator {

  private final DefaultCustomResourceFactory<R> resourceFactory;
  private final CustomResourceFinder<R> finder;
  private final CustomResourceScheduler<R> scheduler;

  private transient JsonPointer targetPointer;

  public AbstractDefaultResourceMutator(DefaultCustomResourceFactory<R> resourceFactory,
      CustomResourceFinder<R> finder, CustomResourceScheduler<R> scheduler) {
    super();
    this.resourceFactory = resourceFactory;
    this.finder = finder;
    this.scheduler = scheduler;
  }

  @PostConstruct
  public void init() throws NoSuchFieldException {
    targetPointer = getTargetPointer();

  }

  @Override
  public List<JsonPatchOperation> mutate(StackgresClusterReview review) {

    if (review.getRequest().getOperation() == Operation.CREATE) {

      R defaultResource = resourceFactory.buildResource();

      StackGresCluster targetCluster = review.getRequest().getObject();
      String targetNamespace = targetCluster.getMetadata().getNamespace();

      String defaultResourceName = defaultResource.getMetadata().getName();

      if (applyDefault(targetCluster)) {

        if (!finder.findByNameAndNamespace(defaultResourceName, targetNamespace).isPresent()) {
          defaultResource.getMetadata().setNamespace(targetNamespace);
          scheduler.create(defaultResource);
        }

        return Collections.singletonList(
            buildAddOperation(targetPointer, defaultResourceName));
      }

    }
    return Collections.emptyList();

  }

  protected boolean applyDefault(StackGresCluster targetCluster) {
    return isTargetPropertyEmpty(targetCluster);
  }

  protected boolean isTargetPropertyEmpty(StackGresCluster targetCluster) {
    return isEmpty(getTargetPropertyValue(targetCluster));
  }

  protected abstract String getTargetPropertyValue(StackGresCluster targetCluster);

  protected abstract JsonPointer getTargetPointer() throws NoSuchFieldException;

}
