/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.dbops;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterExtension;
import io.stackgres.apiweb.dto.cluster.ClusterInstalledExtension;
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DbOpsMinorVersionUpgrade extends AdditionalProperties {

  private String postgresVersion;

  private String method;

  private List<ClusterExtension> postgresExtensions;

  private List<ClusterInstalledExtension> toInstallPostgresExtensions;

  private String statusUpdateDelay;

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getStatusUpdateDelay() {
    return statusUpdateDelay;
  }

  public void setStatusUpdateDelay(String statusUpdateDelay) {
    this.statusUpdateDelay = statusUpdateDelay;
  }

  public List<ClusterExtension> getPostgresExtensions() {
    return postgresExtensions;
  }

  public void setPostgresExtensions(List<ClusterExtension> postgresExtensions) {
    this.postgresExtensions = postgresExtensions;
  }

  public List<ClusterInstalledExtension> getToInstallPostgresExtensions() {
    return toInstallPostgresExtensions;
  }

  public void setToInstallPostgresExtensions(
      List<ClusterInstalledExtension> toInstallPostgresExtensions) {
    this.toInstallPostgresExtensions = toInstallPostgresExtensions;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
