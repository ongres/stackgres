/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigDeveloperPatches {

  private ConfigDeveloperContainerPatches operator;

  private ConfigDeveloperContainerPatches restapi;

  private ConfigDeveloperContainerPatches adminui;

  private ConfigDeveloperContainerPatches jobs;

  private ConfigDeveloperContainerPatches clusterController;

  private ConfigDeveloperContainerPatches distributedlogsController;

  public ConfigDeveloperContainerPatches getOperator() {
    return operator;
  }

  public void setOperator(ConfigDeveloperContainerPatches operator) {
    this.operator = operator;
  }

  public ConfigDeveloperContainerPatches getRestapi() {
    return restapi;
  }

  public void setRestapi(ConfigDeveloperContainerPatches restapi) {
    this.restapi = restapi;
  }

  public ConfigDeveloperContainerPatches getAdminui() {
    return adminui;
  }

  public void setAdminui(ConfigDeveloperContainerPatches adminui) {
    this.adminui = adminui;
  }

  public ConfigDeveloperContainerPatches getJobs() {
    return jobs;
  }

  public void setJobs(ConfigDeveloperContainerPatches jobs) {
    this.jobs = jobs;
  }

  public ConfigDeveloperContainerPatches getClusterController() {
    return clusterController;
  }

  public void setClusterController(ConfigDeveloperContainerPatches clusterController) {
    this.clusterController = clusterController;
  }

  public ConfigDeveloperContainerPatches getDistributedlogsController() {
    return distributedlogsController;
  }

  public void setDistributedlogsController(
      ConfigDeveloperContainerPatches distributedlogsController) {
    this.distributedlogsController = distributedlogsController;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
