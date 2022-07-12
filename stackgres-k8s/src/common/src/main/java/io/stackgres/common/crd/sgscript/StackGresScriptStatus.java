/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresScriptStatus implements KubernetesResource {

  private static final long serialVersionUID = -1L;

  @JsonProperty("scripts")
  @Valid
  private List<StackGresScriptEntryStatus> scripts = new ArrayList<>();

  public List<StackGresScriptEntryStatus> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresScriptEntryStatus> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scripts);
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
    return Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
