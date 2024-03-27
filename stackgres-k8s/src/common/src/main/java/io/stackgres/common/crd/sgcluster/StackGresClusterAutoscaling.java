/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterAutoscaling {

  @ValidEnum(enumClass = StackGresAutoscalingMode.class, allowNulls = false,
      message = "mode must be all, horizontal, vertical or none")
  private String mode;

  private Integer minInstances;

  private Integer maxInstances;

  @Valid
  private Map<String, StackGresClusterAutoscalingVerticalBound> minAllowed;

  @Valid
  private Map<String, StackGresClusterAutoscalingVerticalBound> maxAllowed;

  private StackGresClusterAutoscalingHorizontal horizontal;

  private StackGresClusterAutoscalingVertical vertical;

  @ReferencedField("minInstances")
  interface MinInstances extends FieldReference { }

  @ReferencedField("maxInstances")
  interface MaxInstances extends FieldReference { }

  @ReferencedField("minAllowed")
  interface MinAllowed extends FieldReference { }

  @ReferencedField("maxAllowed")
  interface MaxAllowed extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "minInstances is required when horizontal Pod autoscaling is enabled",
      payload = { MinInstances.class })
  public boolean isMinInstancesPresentWhenHorizontalIsEnabled() {
    return !isHorizontalPodAutoscalingEnabled()
        || minInstances != null;
  }

  @JsonIgnore
  @AssertTrue(message = "maxInstances is required when horizontal Pod autoscaling is enabled",
      payload = { MaxInstances.class })
  public boolean isMaxInstancesPresentWhenHorizontalIsEnabled() {
    return !isHorizontalPodAutoscalingEnabled()
        || maxInstances != null;
  }

  @JsonIgnore
  @AssertTrue(message = "minAllowed is required when vertical Pod autoscaling is enabled",
      payload = { MinAllowed.class })
  public boolean isMinAllowedPresentWhenVerticalIsEnabled() {
    return !isVerticalPodAutoscalingEnabled()
        || minAllowed != null;
  }

  @JsonIgnore
  @AssertTrue(message = "maxAllowed is required when vertical Pod autoscaling is enabled",
      payload = { MaxAllowed.class })
  public boolean isMaxAllowedPresentWhenVerticalIsEnabled() {
    return !isVerticalPodAutoscalingEnabled()
        || maxAllowed != null;
  }

  @JsonIgnore
  @AssertTrue(message = "One or more of patroni, pgbouncer and envoy resources must be secified for minAllowed",
      payload = { MinAllowed.class })
  public boolean isMinAllowedFilledWhenVerticalIsEnabled() {
    return !isVerticalPodAutoscalingEnabled()
        || minAllowed == null
        || (!minAllowed.isEmpty()
            && minAllowed.keySet().stream().allMatch(List.of(
                StackGresContainer.PATRONI.getName(),
                StackGresContainer.PGBOUNCER.getName(),
                StackGresContainer.ENVOY.getName())::contains));
  }

  @JsonIgnore
  @AssertTrue(message = "One or more of patroni, pgbouncer and envoy resources must be secified for maxAllowed",
      payload = { MaxAllowed.class })
  public boolean isMaxAllowedFilledWhenVerticalIsEnabled() {
    return !isVerticalPodAutoscalingEnabled()
        || maxAllowed == null
        || (!maxAllowed.isEmpty()
            && maxAllowed.keySet().stream().allMatch(List.of(
                StackGresContainer.PATRONI.getName(),
                StackGresContainer.PGBOUNCER.getName(),
                StackGresContainer.ENVOY.getName())::contains));
  }

  @JsonIgnore
  public boolean isHorizontalPodAutoscalingEnabled() {
    return mode != null
        && List
        .of(
            StackGresAutoscalingMode.ALL,
            StackGresAutoscalingMode.HORIZONTAL)
        .contains(StackGresAutoscalingMode.fromString(mode));
        
  }

  @JsonIgnore
  public boolean isVerticalPodAutoscalingEnabled() {
    return mode != null
        && List
        .of(
            StackGresAutoscalingMode.ALL,
            StackGresAutoscalingMode.VERTICAL)
        .contains(StackGresAutoscalingMode.fromString(mode));
        
  }

  @JsonIgnore
  public String getMinAllowedForPatroniCpu() {
    return Optional.ofNullable(minAllowed)
        .map(minAllowed -> minAllowed.get(StackGresContainer.PATRONI.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getCpu)
        .orElse(null);
  }

  @JsonIgnore
  public String getMinAllowedForPatroniMemory() {
    return Optional.ofNullable(minAllowed)
        .map(minAllowed -> minAllowed.get(StackGresContainer.PATRONI.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getMemory)
        .orElse(null);
  }

  @JsonIgnore
  public String getMaxAllowedForPatroniCpu() {
    return Optional.ofNullable(maxAllowed)
        .map(maxAllowed -> maxAllowed.get(StackGresContainer.PATRONI.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getCpu)
        .orElse(null);
  }

  @JsonIgnore
  public String getMaxAllowedForPatroniMemory() {
    return Optional.ofNullable(maxAllowed)
        .map(maxAllowed -> maxAllowed.get(StackGresContainer.PATRONI.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getMemory)
        .orElse(null);
  }

  @JsonIgnore
  public String getMinAllowedForPgbouncerCpu() {
    return Optional.ofNullable(minAllowed)
        .map(minAllowed -> minAllowed.get(StackGresContainer.PGBOUNCER.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getCpu)
        .orElse(null);
  }

  @JsonIgnore
  public String getMinAllowedForPgbouncerMemory() {
    return Optional.ofNullable(minAllowed)
        .map(minAllowed -> minAllowed.get(StackGresContainer.PGBOUNCER.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getMemory)
        .orElse(null);
  }

  @JsonIgnore
  public String getMaxAllowedForPgbouncerCpu() {
    return Optional.ofNullable(maxAllowed)
        .map(maxAllowed -> maxAllowed.get(StackGresContainer.PGBOUNCER.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getCpu)
        .orElse(null);
  }

  @JsonIgnore
  public String getMaxAllowedForPgbouncerMemory() {
    return Optional.ofNullable(maxAllowed)
        .map(maxAllowed -> maxAllowed.get(StackGresContainer.PGBOUNCER.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getMemory)
        .orElse(null);
  }

  @JsonIgnore
  public String getMinAllowedForEnvoyCpu() {
    return Optional.ofNullable(minAllowed)
        .map(minAllowed -> minAllowed.get(StackGresContainer.ENVOY.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getCpu)
        .orElse(null);
  }

  @JsonIgnore
  public String getMinAllowedForEnvoyMemory() {
    return Optional.ofNullable(minAllowed)
        .map(minAllowed -> minAllowed.get(StackGresContainer.ENVOY.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getMemory)
        .orElse(null);
  }

  @JsonIgnore
  public String getMaxAllowedForEnvoyCpu() {
    return Optional.ofNullable(maxAllowed)
        .map(maxAllowed -> maxAllowed.get(StackGresContainer.ENVOY.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getCpu)
        .orElse(null);
  }

  @JsonIgnore
  public String getMaxAllowedForEnvoyMemory() {
    return Optional.ofNullable(maxAllowed)
        .map(maxAllowed -> maxAllowed.get(StackGresContainer.ENVOY.getName()))
        .map(StackGresClusterAutoscalingVerticalBound::getMemory)
        .orElse(null);
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public Integer getMinInstances() {
    return minInstances;
  }

  public void setMinInstances(Integer minInstances) {
    this.minInstances = minInstances;
  }

  public Integer getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(Integer maxInstances) {
    this.maxInstances = maxInstances;
  }

  public Map<String, StackGresClusterAutoscalingVerticalBound> getMinAllowed() {
    return minAllowed;
  }

  public void setMinAllowed(Map<String, StackGresClusterAutoscalingVerticalBound> minAllowed) {
    this.minAllowed = minAllowed;
  }

  public Map<String, StackGresClusterAutoscalingVerticalBound> getMaxAllowed() {
    return maxAllowed;
  }

  public void setMaxAllowed(Map<String, StackGresClusterAutoscalingVerticalBound> maxAllowed) {
    this.maxAllowed = maxAllowed;
  }

  public StackGresClusterAutoscalingHorizontal getHorizontal() {
    return horizontal;
  }

  public void setHorizontal(StackGresClusterAutoscalingHorizontal horizontal) {
    this.horizontal = horizontal;
  }

  public StackGresClusterAutoscalingVertical getVertical() {
    return vertical;
  }

  public void setVertical(StackGresClusterAutoscalingVertical vertical) {
    this.vertical = vertical;
  }

  @Override
  public int hashCode() {
    return Objects.hash(horizontal, maxAllowed, maxInstances, minAllowed, minInstances, mode, vertical);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterAutoscaling)) {
      return false;
    }
    StackGresClusterAutoscaling other = (StackGresClusterAutoscaling) obj;
    return Objects.equals(horizontal, other.horizontal) && Objects.equals(maxAllowed, other.maxAllowed)
        && Objects.equals(maxInstances, other.maxInstances) && Objects.equals(minAllowed, other.minAllowed)
        && Objects.equals(minInstances, other.minInstances) && Objects.equals(mode, other.mode)
        && Objects.equals(vertical, other.vertical);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
