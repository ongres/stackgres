/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterDbOpsMajorVersionUpgradeStatus {

  private List<String> initialInstances;

  private String primaryInstance;

  private String sourcePostgresVersion;

  private List<ClusterExtension> sourcePostgresExtensions;

  private String sourceSgPostgresConfig;

  private String sourceBackupPath;

  private String targetPostgresVersion;

  private String locale;

  private String encoding;

  private Boolean dataChecksum;

  private Boolean link;

  private Boolean clone;

  private Boolean check;

  private Boolean rollback;

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

  public List<ClusterExtension> getSourcePostgresExtensions() {
    return sourcePostgresExtensions;
  }

  public void setSourcePostgresExtensions(
      List<ClusterExtension> sourcePostgresExtensions) {
    this.sourcePostgresExtensions = sourcePostgresExtensions;
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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
