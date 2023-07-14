/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterDbOpsMajorVersionUpgradeStatus extends ClusterDbOpsRestartStatus {

  @JsonProperty("sourcePostgresVersion")
  @NotNull
  private String sourcePostgresVersion;

  @JsonProperty("sourceSgPostgresConfig")
  @NotNull
  private String sourceSgPostgresConfig;

  @JsonProperty("sourceBackupPath")
  private String sourceBackupPath;

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

  @JsonProperty("rollback")
  private Boolean rollback;

  public String getSourcePostgresVersion() {
    return sourcePostgresVersion;
  }

  public void setSourcePostgresVersion(String sourcePostgresVersion) {
    this.sourcePostgresVersion = sourcePostgresVersion;
  }

  public String getSourceSgPostgresConfig() {
    return sourceSgPostgresConfig;
  }

  public void setSourceSgPostgresConfig(String sourceSgPostgresConfig) {
    this.sourceSgPostgresConfig = sourceSgPostgresConfig;
  }

  public String getSourceBackupPath() {
    return sourceBackupPath;
  }

  public void setSourceBackupPath(String sourceBackupPath) {
    this.sourceBackupPath = sourceBackupPath;
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

  public Boolean getRollback() {
    return rollback;
  }

  public void setRollback(Boolean rollback) {
    this.rollback = rollback;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result + Objects.hash(check, clone, dataChecksum, encoding, link, locale, rollback,
            sourceBackupPath, sourcePostgresVersion, sourceSgPostgresConfig, targetPostgresVersion);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof StackGresClusterDbOpsMajorVersionUpgradeStatus)) {
      return false;
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus other =
        (StackGresClusterDbOpsMajorVersionUpgradeStatus) obj;
    return Objects.equals(check, other.check) && Objects.equals(clone, other.clone)
        && Objects.equals(dataChecksum, other.dataChecksum)
        && Objects.equals(encoding, other.encoding) && Objects.equals(link, other.link)
        && Objects.equals(locale, other.locale) && Objects.equals(rollback, other.rollback)
        && Objects.equals(sourceBackupPath, other.sourceBackupPath)
        && Objects.equals(sourcePostgresVersion, other.sourcePostgresVersion)
        && Objects.equals(sourceSgPostgresConfig, other.sourceSgPostgresConfig)
        && Objects.equals(targetPostgresVersion, other.targetPostgresVersion);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
