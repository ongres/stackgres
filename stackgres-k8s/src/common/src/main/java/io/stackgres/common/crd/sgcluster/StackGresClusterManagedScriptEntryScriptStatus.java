/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterManagedScriptEntryScriptStatus {

  @JsonProperty("id")
  @NotNull(message = "id can not be null")
  private Integer id;

  @JsonProperty("version")
  @NotNull(message = "version can not be null")
  private Integer version;

  @JsonProperty("intents")
  private Integer intents;

  @JsonProperty("failureCode")
  private String failureCode;

  @JsonProperty("failure")
  private String failure;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Integer getIntents() {
    return intents;
  }

  public void setIntents(Integer intents) {
    this.intents = intents;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public void setFailureCode(String failureCode) {
    this.failureCode = failureCode;
  }

  public String getFailure() {
    return failure;
  }

  public void setFailure(String failure) {
    this.failure = failure;
  }

  @Override
  public int hashCode() {
    return Objects.hash(failure, failureCode, id, intents, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterManagedScriptEntryScriptStatus)) {
      return false;
    }
    StackGresClusterManagedScriptEntryScriptStatus other =
        (StackGresClusterManagedScriptEntryScriptStatus) obj;
    return Objects.equals(failure, other.failure) && Objects.equals(failureCode, other.failureCode)
        && Objects.equals(id, other.id) && Objects.equals(intents, other.intents)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
