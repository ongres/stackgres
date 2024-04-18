/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path(MutationUtil.SCRIPT_MUTATION_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ScriptMutationResource
    extends AbstractMutationResource<StackGresScript, StackGresScriptReview> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ScriptMutationResource.class);

  @Inject
  public ScriptMutationResource(
      ObjectMapper objectMapper,
      MutationPipeline<StackGresScript, StackGresScriptReview> pipeline) {
    super(OperatorProperty.getAllowedNamespaces(), objectMapper, pipeline);
  }

  public ScriptMutationResource() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
  }

  void onStart(@Observes StartupEvent ev) {
    LOGGER.info("Script mutation resource started");
  }

  @POST
  @Override
  public AdmissionReviewResponse mutate(StackGresScriptReview admissionReview) {
    return super.mutate(admissionReview);
  }

  @Override
  protected Class<StackGresScript> getResourceClass() {
    return StackGresScript.class;
  }
}
