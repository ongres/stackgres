/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ConversionUtil.DBOPS_CONVERSION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SgDbOpsConversionResource implements ConversionResource {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SgDbOpsConversionResource.class);

  private final ConversionPipeline pipeline;

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("SGDbOps configuration conversion resource started");
  }

  @Inject
  public SgDbOpsConversionResource(
      @Conversion(StackGresDbOps.KIND) ConversionPipeline pipeline) {
    this.pipeline = pipeline;
  }

  @POST
  public ConversionReviewResponse convert(ConversionReview conversionReview) {

    return convert(pipeline, conversionReview);

  }
}
