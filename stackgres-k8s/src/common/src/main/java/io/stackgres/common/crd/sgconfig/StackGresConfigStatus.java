/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigStatus {

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Valid
  private List<Condition> conditions = new ArrayList<>();

  private String version;

  private Boolean removeOldOperatorBundleResources;

  private StackGresConfigStatusGrafana grafana;

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public Boolean getRemoveOldOperatorBundleResources() {
    return removeOldOperatorBundleResources;
  }

  public void setRemoveOldOperatorBundleResources(Boolean removeOldOperatorBundleResources) {
    this.removeOldOperatorBundleResources = removeOldOperatorBundleResources;
  }

  public StackGresConfigStatusGrafana getGrafana() {
    return grafana;
  }

  public void setGrafana(StackGresConfigStatusGrafana grafana) {
    this.grafana = grafana;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, grafana, removeOldOperatorBundleResources, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigStatus)) {
      return false;
    }
    StackGresConfigStatus other = (StackGresConfigStatus) obj;
    return Objects.equals(conditions, other.conditions) && Objects.equals(grafana, other.grafana)
        && Objects.equals(removeOldOperatorBundleResources, other.removeOldOperatorBundleResources)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
