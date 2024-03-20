/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.keda;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class ScaledObjectTrigger {

  private String type;

  private String name;

  private String metricType;

  private Boolean useCachedMetrics;

  private Map<String, String> metadata;

  private ScaledObjectAuthenticationRef authenticationRef;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMetricType() {
    return metricType;
  }

  public void setMetricType(String metricType) {
    this.metricType = metricType;
  }

  public Boolean getUseCachedMetrics() {
    return useCachedMetrics;
  }

  public void setUseCachedMetrics(Boolean useCachedMetrics) {
    this.useCachedMetrics = useCachedMetrics;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public ScaledObjectAuthenticationRef getAuthenticationRef() {
    return authenticationRef;
  }

  public void setAuthenticationRef(ScaledObjectAuthenticationRef authenticationRef) {
    this.authenticationRef = authenticationRef;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authenticationRef, metadata, metricType, name, type, useCachedMetrics);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ScaledObjectTrigger)) {
      return false;
    }
    ScaledObjectTrigger other = (ScaledObjectTrigger) obj;
    return Objects.equals(authenticationRef, other.authenticationRef) && Objects.equals(metadata, other.metadata)
        && Objects.equals(metricType, other.metricType) && Objects.equals(name, other.name)
        && Objects.equals(type, other.type) && Objects.equals(useCachedMetrics, other.useCachedMetrics);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
