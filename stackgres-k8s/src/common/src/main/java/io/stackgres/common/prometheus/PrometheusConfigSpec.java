/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.prometheus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrometheusConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  private LabelSelector serviceMonitorSelector;

  public LabelSelector getServiceMonitorSelector() {
    return serviceMonitorSelector;
  }

  public void setServiceMonitorSelector(LabelSelector serviceMonitorSelector) {
    this.serviceMonitorSelector = serviceMonitorSelector;
  }

}
