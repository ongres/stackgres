/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

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
public class StackGresClusterDbOpsMajorVersionUpgradeStatus implements KubernetesResource {

  private static final long serialVersionUID = -1;

  @JsonProperty("initialInstances")
  @NotNull
  private String initialInstances;

  @JsonProperty("primaryInstance")
  @NotNull
  private String primaryInstance;

  @JsonProperty("sourcePostgresVersion")
  @NotNull
  private String sourcePostgresVersion;

  @JsonProperty("targetPostgresVersion")
  @NotNull
  private String targetPostgresVersion;

  @JsonProperty("locale")
  @NotNull
  private String locale;

  @JsonProperty("encoding")
  @NotNull
  private String encoding;

  @JsonProperty("dataChecksum")
  @NotNull
  private Boolean dataChecksum;

  @JsonProperty("link")
  @NotNull
  private Boolean link;

  @JsonProperty("clone")
  @NotNull
  private Boolean clone;

  @JsonProperty("check")
  @NotNull
  private Boolean check;

  public String getInitialInstances() {
    return initialInstances;
  }

  public void setInitialInstances(String initialInstances) {
    this.initialInstances = initialInstances;
  }

  public String getPrimaryInstance() {
    return primaryInstance;
  }

  public void setPrimaryInstance(String primaryInstance) {
    this.primaryInstance = primaryInstance;
  }

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getTargetPostgresVersion() {
    return targetPostgresVersion;
  }

  public void setTargetPostgresVersion(String targetPostgresVersion) {
    this.targetPostgresVersion = targetPostgresVersion;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public Boolean getDataChecksum() {
    return dataChecksum;
  }

  public void setDataChecksum(Boolean dataChecksum) {
    this.dataChecksum = dataChecksum;
  }

  public Boolean getLink() {
    return link;
  }

  public void setLink(Boolean link) {
    this.link = link;
  }

  public Boolean getClone() {
    return clone;
  }

  public void setClone(Boolean clone) {
    this.clone = clone;
  }

  public Boolean getCheck() {
    return check;
  }

  public void setCheck(Boolean check) {
    this.check = check;
  }

  @Override
  public int hashCode() {
    return Objects.hash(check, clone, dataChecksum, encoding, initialInstances, link, locale,
        primaryInstance, sourcePostgresVersion, targetPostgresVersion);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterDbOpsMajorVersionUpgradeStatus)) {
      return false;
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus other =
        (StackGresClusterDbOpsMajorVersionUpgradeStatus) obj;
    return Objects.equals(check, other.check) && Objects.equals(clone, other.clone)
        && Objects.equals(dataChecksum, other.dataChecksum)
        && Objects.equals(encoding, other.encoding)
        && Objects.equals(initialInstances, other.initialInstances)
        && Objects.equals(link, other.link) && Objects.equals(locale, other.locale)
        && Objects.equals(primaryInstance, other.primaryInstance)
        && Objects.equals(sourcePostgresVersion, other.sourcePostgresVersion)
        && Objects.equals(targetPostgresVersion, other.targetPostgresVersion);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
