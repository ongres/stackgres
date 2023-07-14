/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomServicePort;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterPostgresCoordinatorServices {

  @NotNull(message = "any is required")
  private StackGresPostgresService any;

  @NotNull(message = "primary is required")
  private StackGresPostgresService primary;

  @Valid
  private List<CustomServicePort> customPorts;

  public StackGresPostgresService getAny() {
    return any;
  }

  public void setAny(StackGresPostgresService any) {
    this.any = any;
  }

  public StackGresPostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(StackGresPostgresService primary) {
    this.primary = primary;
  }

  public List<CustomServicePort> getCustomPorts() {
    return customPorts;
  }

  public void setCustomPorts(List<CustomServicePort> customPorts) {
    this.customPorts = customPorts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(any, customPorts, primary);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterPostgresCoordinatorServices)) {
      return false;
    }
    StackGresShardedClusterPostgresCoordinatorServices other =
        (StackGresShardedClusterPostgresCoordinatorServices) obj;
    return Objects.equals(any, other.any) && Objects.equals(customPorts, other.customPorts)
        && Objects.equals(primary, other.primary);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
