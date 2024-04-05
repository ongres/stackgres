/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class PrometheusSpec {

  private LabelSelector podMonitorSelector;

  public LabelSelector getPodMonitorSelector() {
    return podMonitorSelector;
  }

  public void setPodMonitorSelector(LabelSelector podMonitorSelector) {
    this.podMonitorSelector = podMonitorSelector;
  }

  @Override
  public int hashCode() {
    return Objects.hash(podMonitorSelector);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PrometheusSpec)) {
      return false;
    }
    PrometheusSpec other = (PrometheusSpec) obj;
    return Objects.equals(podMonitorSelector, other.podMonitorSelector);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
