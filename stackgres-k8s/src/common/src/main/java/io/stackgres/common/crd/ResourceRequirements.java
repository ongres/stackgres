/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.Quantity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
        @BuildableReference(io.fabric8.kubernetes.api.model.ResourceRequirements.class)
    })
@SuppressFBWarnings(value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification = "Intentional name shadowing")
public class ResourceRequirements extends io.fabric8.kubernetes.api.model.ResourceRequirements {

  private static final long serialVersionUID = 1L;

  public ResourceRequirements() {
    super();
  }

  public ResourceRequirements(
      Map<String, Quantity> limits,
      Map<String, Quantity> requests) {
    super(limits, requests);
  }

  @Override
  public Map<String, Quantity> getLimits() {
    return super.getLimits();
  }

  @Override
  public void setLimits(Map<String, Quantity> limits) {
    super.setLimits(limits);
  }

  @Override
  public Map<String, Quantity> getRequests() {
    return super.getRequests();
  }

  @Override
  public void setRequests(Map<String, Quantity> requests) {
    super.setRequests(requests);
  }

}