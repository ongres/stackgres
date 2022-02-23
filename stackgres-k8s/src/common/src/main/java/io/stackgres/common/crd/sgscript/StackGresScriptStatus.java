/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresScriptStatus implements KubernetesResource {

  private static final long serialVersionUID = -1L;

  @JsonProperty("lastId")
  @NotNull(message = "lastId cannot be null")
  private Integer lastId;

  @JsonProperty("scripts")
  @Valid
  private List<StackGresScriptEntryStatus> scripts = new ArrayList<>();

  public Integer getLastId() {
    return lastId;
  }

  public void setLastId(Integer lastId) {
    this.lastId = lastId;
  }

  public List<StackGresScriptEntryStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresScriptEntryStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lastId, scripts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresScriptStatus)) {
      return false;
    }
    StackGresScriptStatus other = (StackGresScriptStatus) obj;
    return Objects.equals(lastId, other.lastId) && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
