/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ConversionUtil.CONNPOOLCONFIG_CONVERSION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SgPoolingConfigConversionResource implements ConversionResource {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SgPoolingConfigConversionResource.class);

  private final ConversionPipeline pipeline;

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("SGPoolingConfig configuration conversion resource started");
  }

  @Inject
  public SgPoolingConfigConversionResource(
      @Conversion(StackGresPoolingConfig.KIND) ConversionPipeline pipeline) {
    this.pipeline = pipeline;
  }

  @Override
  @POST
  public ConversionReviewResponse convert(ConversionReview conversionReview) {
    return convert(pipeline, conversionReview);
  }
}
