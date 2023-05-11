/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterInitData {

  @JsonProperty("restore")
  @Valid
  private StackGresClusterRestore restore;

  @JsonProperty("scripts")
  @Null(message = "scripts section is deprecated,"
      + " please use .spec.managedSql.scripts section instead.")
  private List<StackGresClusterScriptEntry> scripts;

  public StackGresClusterRestore getRestore() {
    return restore;
  }

  public void setRestore(StackGresClusterRestore restore) {
    this.restore = restore;
  }

  public List<StackGresClusterScriptEntry> getScripts() {
    return scripts;
  }

  public void setScripts(List<StackGresClusterScriptEntry> scripts) {
    this.scripts = scripts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(restore, scripts);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterInitData)) {
      return false;
    }
    StackGresClusterInitData other = (StackGresClusterInitData) obj;
    return Objects.equals(restore, other.restore) && Objects.equals(scripts, other.scripts);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
