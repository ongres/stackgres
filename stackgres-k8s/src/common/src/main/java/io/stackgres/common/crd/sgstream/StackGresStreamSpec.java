/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamSpec {

  @Valid
  @NotNull
  private StackGresStreamSource source;

  @Valid
  @NotNull
  private StackGresStreamTarget target;

  @Min(value = -1, message = "maxRetries must be greather or equals to -1.")
  private Integer maxRetries;

  @Valid
  private StackGresStreamPods pods;

  @Valid
  private StackGresStreamDebeziumEngineProperties debeziumEngineProperties;

  private Boolean useDebeziumAsyncEngine;

  public StackGresStreamSource getSource() {
    return source;
  }

  public void setSource(StackGresStreamSource source) {
    this.source = source;
  }

  public StackGresStreamTarget getTarget() {
    return target;
  }

  public void setTarget(StackGresStreamTarget target) {
    this.target = target;
  }

  public Integer getMaxRetries() {
    return maxRetries;
  }

  public void setMaxRetries(Integer maxRetries) {
    this.maxRetries = maxRetries;
  }

  public StackGresStreamPods getPods() {
    return pods;
  }

  public void setPods(StackGresStreamPods pods) {
    this.pods = pods;
  }

  public StackGresStreamDebeziumEngineProperties getDebeziumEngineProperties() {
    return debeziumEngineProperties;
  }

  public void setDebeziumEngineProperties(
      StackGresStreamDebeziumEngineProperties debeziumEngineProperties) {
    this.debeziumEngineProperties = debeziumEngineProperties;
  }

  public Boolean getUseDebeziumAsyncEngine() {
    return useDebeziumAsyncEngine;
  }

  public void setUseDebeziumAsyncEngine(Boolean useDebeziumAsyncEngine) {
    this.useDebeziumAsyncEngine = useDebeziumAsyncEngine;
  }

  @Override
  public int hashCode() {
    return Objects.hash(debeziumEngineProperties, maxRetries, pods, source, target,
        useDebeziumAsyncEngine);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamSpec)) {
      return false;
    }
    StackGresStreamSpec other = (StackGresStreamSpec) obj;
    return Objects.equals(debeziumEngineProperties, other.debeziumEngineProperties)
        && Objects.equals(maxRetries, other.maxRetries) && Objects.equals(pods, other.pods)
        && Objects.equals(source, other.source) && Objects.equals(target, other.target)
        && Objects.equals(useDebeziumAsyncEngine, other.useDebeziumAsyncEngine);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
