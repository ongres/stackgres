/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDbOpsVacuum extends StackGresDbOpsVacuumConfig {

  @JsonProperty("databases")
  private List<StackGresDbOpsVacuumDatabase> databases;

  public List<StackGresDbOpsVacuumDatabase> getDatabases() {
    return databases;
  }

  public void setDatabases(List<StackGresDbOpsVacuumDatabase> databases) {
    this.databases = databases;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(databases);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresDbOpsVacuum)) {
      return false;
    }
    StackGresDbOpsVacuum other = (StackGresDbOpsVacuum) obj;
    return Objects.equals(databases, other.databases);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
