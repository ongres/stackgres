/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterDbOpsMajorVersionUpgradeStatus {

  @JsonProperty("initialInstances")
  private List<String> initialInstances;

  @JsonProperty("primaryInstance")
  private String primaryInstance;

  @JsonProperty("sourcePostgresVersion")
  private String sourcePostgresVersion;

  @JsonProperty("targetPostgresVersion")
  private String targetPostgresVersion;

  @JsonProperty("locale")
  private String locale;

  @JsonProperty("encoding")
  private String encoding;

  @JsonProperty("dataChecksum")
  private Boolean dataChecksum;

  @JsonProperty("link")
  private Boolean link;

  @JsonProperty("clone")
  private Boolean clone;

  @JsonProperty("check")
  private Boolean check;

  public List<String> getInitialInstances() {
    return initialInstances;
  }

  public void setInitialInstances(List<String> initialInstances) {
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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterDbOpsMajorVersionUpgradeStatus that = (ClusterDbOpsMajorVersionUpgradeStatus) o;
    return Objects.equals(initialInstances, that.initialInstances)
        && Objects.equals(primaryInstance, that.primaryInstance)
        && Objects.equals(sourcePostgresVersion, that.sourcePostgresVersion)
        && Objects.equals(targetPostgresVersion, that.targetPostgresVersion)
        && Objects.equals(locale, that.locale)
        && Objects.equals(encoding, that.encoding)
        && Objects.equals(dataChecksum, that.dataChecksum)
        && Objects.equals(link, that.link) && Objects.equals(clone, that.clone)
        && Objects.equals(check, that.check);
  }

  @Override
  public int hashCode() {
    return Objects.hash(initialInstances, primaryInstance, sourcePostgresVersion,
        targetPostgresVersion, locale, encoding, dataChecksum, link, clone, check);
  }
}
