/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardeddbops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedDbOpsMajorVersionUpgrade {

  private String postgresVersion;

  private String sgPostgresConfig;

  private List<String> backupPaths;

  private Boolean link;

  private Boolean clone;

  private Boolean check;

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getSgPostgresConfig() {
    return sgPostgresConfig;
  }

  public void setSgPostgresConfig(String sgPostgresConfig) {
    this.sgPostgresConfig = sgPostgresConfig;
  }

  public List<String> getBackupPaths() {
    return backupPaths;
  }

  public void setBackupPath(List<String> backupPaths) {
    this.backupPaths = backupPaths;
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

}
