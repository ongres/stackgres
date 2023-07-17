/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotEmpty;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsMinorVersionUpgrade {

  @JsonProperty("postgresVersion")
  @NotEmpty(message = "spec.minorVersionUpgrade.postgresVersion must not be empty")
  private String postgresVersion;

  @JsonProperty("method")
  @ValidEnum(enumClass = DbOpsMethodType.class, allowNulls = true,
      message = "method must be InPlace or ReducedImpact")
  private String method;

  @JsonIgnore
  public boolean isMethodReducedImpact() {
    return Objects.equals(method, DbOpsMethodType.REDUCED_IMPACT.toString());
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  @Override
  public int hashCode() {
    return Objects.hash(method, postgresVersion);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsMinorVersionUpgrade)) {
      return false;
    }
    StackGresDbOpsMinorVersionUpgrade other = (StackGresDbOpsMinorVersionUpgrade) obj;
    return Objects.equals(method, other.method)
        && Objects.equals(postgresVersion, other.postgresVersion);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
