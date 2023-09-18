/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conversion;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(ConversionUtil.CONFIG_CONVERSION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SgConfigConversionResource implements ConversionResource {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SgConfigConversionResource.class);

  private final ConversionPipeline pipeline;

  @Inject
  public SgConfigConversionResource(
      @Conversion(StackGresConfig.KIND) ConversionPipeline pipeline) {
    this.pipeline = pipeline;
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("SgConfig configuration conversion resource started");
  }

  @Override
  @POST
  public ConversionReviewResponse convert(ConversionReview conversionReview) {
    return convert(pipeline, conversionReview);
  }
}
