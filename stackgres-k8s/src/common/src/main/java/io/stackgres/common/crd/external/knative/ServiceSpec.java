/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.knative;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ServiceSpec {

  private PodTemplateSpec template;

  private List<ServiceTraffic> traffic;

  public PodTemplateSpec getTemplate() {
    return template;
  }

  public void setTemplate(PodTemplateSpec template) {
    this.template = template;
  }

  public List<ServiceTraffic> getTraffic() {
    return traffic;
  }

  public void setTraffic(List<ServiceTraffic> traffic) {
    this.traffic = traffic;
  }

  @Override
  public int hashCode() {
    return Objects.hash(template, traffic);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ServiceSpec)) {
      return false;
    }
    ServiceSpec other = (ServiceSpec) obj;
    return Objects.equals(template, other.template) && Objects.equals(traffic, other.traffic);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
