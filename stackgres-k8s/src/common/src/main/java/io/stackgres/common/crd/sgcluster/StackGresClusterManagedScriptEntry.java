/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.NotNull;

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
public class StackGresClusterManagedScriptEntry {

  @JsonProperty("id")
  @NotNull(message = "id can not be null")
  private Integer id;

  @JsonProperty("sgScript")
  @NotNull(message = "sgScript can not be null")
  private String sgScript;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getSgScript() {
    return sgScript;
  }

  public void setSgScript(String sgScript) {
    this.sgScript = sgScript;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sgScript);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterManagedScriptEntry)) {
      return false;
    }
    StackGresClusterManagedScriptEntry other = (StackGresClusterManagedScriptEntry) obj;
    return Objects.equals(id, other.id) && Objects.equals(sgScript, other.sgScript);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
