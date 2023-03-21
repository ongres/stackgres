/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.initialization.DefaultCustomResourceFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class DefaultProfileMutator
    extends AbstractDefaultResourceMutator<StackGresProfile,
        StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  @Inject
  public DefaultProfileMutator(
      DefaultCustomResourceFactory<StackGresProfile> resourceFactory,
      CustomResourceFinder<StackGresProfile> finder,
      CustomResourceScheduler<StackGresProfile> scheduler) {
    super(resourceFactory, finder, scheduler);
  }

  public DefaultProfileMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  @PostConstruct
  @Override
  public void init() {
    super.init();
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresDistributedLogsReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      operations.addAll(super.mutate(review));
      return operations.build();
    }
    return List.of();
  }

  @Override
  protected String getTargetPropertyValue(StackGresDistributedLogs targetDistributedLogs) {
    return targetDistributedLogs.getSpec().getResourceProfile();
  }

  @Override
  protected JsonPointer getTargetPointer() {
    return getTargetPointer("resourceProfile");
  }
}
