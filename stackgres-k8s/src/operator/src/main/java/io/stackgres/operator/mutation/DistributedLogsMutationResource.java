/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.mutating.AbstractMutationResource;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(MutationUtil.DISTRIBUTED_LOGS_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DistributedLogsMutationResource
    extends AbstractMutationResource<StackGresDistributedLogs, StackGresDistributedLogsReview> {

  @Inject
  public DistributedLogsMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresDistributedLogs, StackGresDistributedLogsReview> pipeline) {
    super(OperatorProperty.getAllowedNamespaces(), objectMapper, pipeline);
  }

  public DistributedLogsMutationResource() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    getLogger().info("Distributed logs mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(StackGresDistributedLogsReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresDistributedLogs> getResourceClass() {
    return StackGresDistributedLogs.class;
  }
}
