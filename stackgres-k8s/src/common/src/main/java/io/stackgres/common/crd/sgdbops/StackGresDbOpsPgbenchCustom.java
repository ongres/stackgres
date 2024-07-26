/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdbops;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresDbOpsPgbenchCustom {

  @Valid
  private StackGresDbOpsPgbenchCustomScript initialization;

  private List<StackGresDbOpsPgbenchCustomScript> scripts;

  public StackGresDbOpsPgbenchCustomScript getInitialization() {
    return initialization;
  }

  public void setInitialization(StackGresDbOpsPgbenchCustomScript initialization) {
    this.initialization = initialization;
  }

  public List<StackGresDbOpsPgbenchCustomScript> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresDbOpsPgbenchCustomScript> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialization, scripts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDbOpsPgbenchCustom)) {
      return false;
    }
    StackGresDbOpsPgbenchCustom other = (StackGresDbOpsPgbenchCustom) obj;
    return Objects.equals(initialization, other.initialization)
        && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
