/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ValidationUtil.SHARDED_CLUSTER_VALIDATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ShardedClusterValidationResource
    extends AbstractValidationResource<StackGresShardedClusterReview> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ShardedClusterValidationResource.class);

  @Inject
  public ShardedClusterValidationResource(
      ValidationPipeline<StackGresShardedClusterReview> pipeline) {
    super(pipeline);
  }

  public ShardedClusterValidationResource() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Sharded Cluster validation resource started");
  }

  /**
   * Admission Web hook callback.
   */
  @POST
  public AdmissionReviewResponse validate(StackGresShardedClusterReview admissionReview) {
    return super.validate(admissionReview);

  }

}
