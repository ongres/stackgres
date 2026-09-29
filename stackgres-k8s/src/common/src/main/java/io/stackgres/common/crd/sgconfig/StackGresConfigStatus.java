/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigStatus extends AdditionalProperties {

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<@Valid Condition> conditions = new ArrayList<>();

  private String version;

  private String installationId;

  private Boolean oldOperatorBundleResourcesRemoved;

  private StackGresConfigStatusGrafana grafana;

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

  public String getInstallationId() {
    return installationId;
  }

  public void setInstallationId(String installationId) {
    this.installationId = installationId;
  }

  public Boolean getOldOperatorBundleResourcesRemoved() {
    return oldOperatorBundleResourcesRemoved;
  }

  public void setOldOperatorBundleResourcesRemoved(Boolean oldOperatorBundleResourcesRemoved) {
    this.oldOperatorBundleResourcesRemoved = oldOperatorBundleResourcesRemoved;
  }

  public StackGresConfigStatusGrafana getGrafana() {
    return grafana;
  }

  public void setGrafana(StackGresConfigStatusGrafana grafana) {
    this.grafana = grafana;
  }

  public String getExistingCrUpdatedToVersion() {
    return existingCrUpdatedToVersion;
  }

  public void setExistingCrUpdatedToVersion(String existingCrUpdatedToVersion) {
    this.existingCrUpdatedToVersion = existingCrUpdatedToVersion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, existingCrUpdatedToVersion, grafana, installationId,
        oldOperatorBundleResourcesRemoved, version);
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
    return Objects.equals(conditions, other.conditions)
        && Objects.equals(existingCrUpdatedToVersion, other.existingCrUpdatedToVersion)
        && Objects.equals(grafana, other.grafana)
        && Objects.equals(installationId, other.installationId) && Objects
            .equals(oldOperatorBundleResourcesRemoved, other.oldOperatorBundleResourcesRemoved)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
