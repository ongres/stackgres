/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterCitusConfigurations extends AdditionalProperties {

  public static final Duration DEFAULT_UPDATE_NODE_INTERVAL = Duration.ofSeconds(10);

  private String updateNodeInterval;

  private Boolean enableNodeAutoRemoval;

  private Boolean connectToPooler;

  @ReferencedField("updateNodeInterval")
  interface UpdateNodeInterval extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "updateNodeInterval must be at least 1 second and in ISO 8601 duration"
      + " format: `PnDTnHnMn.nS`.",
      payload = UpdateNodeInterval.class)
  public boolean isUpdateNodeIntervalValid() {
    try {
      return updateNodeInterval == null
          || Duration.parse(updateNodeInterval).compareTo(Duration.ofSeconds(1)) >= 0;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

  public String getUpdateNodeInterval() {
    return updateNodeInterval;
  }

  @JsonIgnore
  public Duration getUpdateNodeIntervalOrDefault() {
    return Optional.ofNullable(updateNodeInterval)
        .map(Duration::parse)
        .orElse(DEFAULT_UPDATE_NODE_INTERVAL);
  }

  public void setUpdateNodeInterval(String updateNodeInterval) {
    this.updateNodeInterval = updateNodeInterval;
  }

  public Boolean getEnableNodeAutoRemoval() {
    return enableNodeAutoRemoval;
  }

  @JsonIgnore
  public boolean getEnableNodeAutoRemovalOrDefault() {
    return Optional.ofNullable(enableNodeAutoRemoval).orElse(false);
  }

  public void setEnableNodeAutoRemoval(Boolean enableNodeAutoRemoval) {
    this.enableNodeAutoRemoval = enableNodeAutoRemoval;
  }

  public Boolean getConnectToPooler() {
    return connectToPooler;
  }

  @JsonIgnore
  public boolean getConnectToPoolerOrDefault() {
    return Optional.ofNullable(connectToPooler).orElse(true);
  }

  public void setConnectToPooler(Boolean connectToPooler) {
    this.connectToPooler = connectToPooler;
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectToPooler, enableNodeAutoRemoval, updateNodeInterval);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterCitusConfigurations)) {
      return false;
    }
    StackGresShardedClusterCitusConfigurations other =
        (StackGresShardedClusterCitusConfigurations) obj;
    return Objects.equals(connectToPooler, other.connectToPooler)
        && Objects.equals(enableNodeAutoRemoval, other.enableNodeAutoRemoval)
        && Objects.equals(updateNodeInterval, other.updateNodeInterval);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
