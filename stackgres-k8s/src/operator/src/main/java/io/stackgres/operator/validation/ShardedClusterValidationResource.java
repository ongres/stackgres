/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReviewResponse;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
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
