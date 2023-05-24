/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CustomServicePort;
import io.stackgres.common.crd.postgres.service.StackGresPostgresService;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterPostgresShardsServices {

  @NotNull(message = "primaries is required")
  private StackGresPostgresService primaries;

  @Valid
  private List<CustomServicePort> customPorts;

  public StackGresPostgresService getPrimaries() {
    return primaries;
  }

  public void setPrimaries(StackGresPostgresService primaries) {
    this.primaries = primaries;
  }

  public List<CustomServicePort> getCustomPorts() {
    return customPorts;
  }

  public void setCustomPorts(List<CustomServicePort> customPorts) {
    this.customPorts = customPorts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(primaries, customPorts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterPostgresShardsServices)) {
      return false;
    }
    StackGresShardedClusterPostgresShardsServices other =
        (StackGresShardedClusterPostgresShardsServices) obj;
    return Objects.equals(primaries, other.primaries)
        && Objects.equals(customPorts, other.customPorts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
