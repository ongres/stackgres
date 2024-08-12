/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigStatus {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<Condition> conditions = new ArrayList<>();

  private String version;

  private Boolean removeOldOperatorBundleResource;

  private ConfigStatusGrafana grafana;

  private String existingCrUpdatedToVersion;

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

  public Boolean getRemoveOldOperatorBundleResource() {
    return removeOldOperatorBundleResource;
  }

  public void setRemoveOldOperatorBundleResource(Boolean removeOldOperatorBundleResource) {
    this.removeOldOperatorBundleResource = removeOldOperatorBundleResource;
  }

  public ConfigStatusGrafana getGrafana() {
    return grafana;
  }

  public void setGrafana(ConfigStatusGrafana grafana) {
    this.grafana = grafana;
  }

  public String getExistingCrUpdatedToVersion() {
    return existingCrUpdatedToVersion;
  }

  public void setExistingCrUpdatedToVersion(String existingCrUpdatedToVersion) {
    this.existingCrUpdatedToVersion = existingCrUpdatedToVersion;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
