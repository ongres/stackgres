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
public class ConfigSpec {

  private String containerRegistry;

  private String imagePullPolicy;

  private ConfigOptionalServiceAccount serviceAccount;

  private String initClusterRole;

  private ConfigOperator operator;

  private ConfigRestapi restapi;

  private ConfigWebConsole adminui;

  private ConfigJobs jobs;

  private ConfigDeploy deploy;

  private ConfigAuthentication authentication;

  private ConfigPrometheus prometheus;

  private ConfigGrafana grafana;

  private ConfigExtensions extensions;

  private ConfigDeveloper developer;

  public String getContainerRegistry() {
    return containerRegistry;
  }

  public void setContainerRegistry(String containerRegistry) {
    this.containerRegistry = containerRegistry;
  }

  public String getImagePullPolicy() {
    return imagePullPolicy;
  }

  public void setImagePullPolicy(String imagePullPolicy) {
    this.imagePullPolicy = imagePullPolicy;
  }

  public ConfigOptionalServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ConfigOptionalServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public String getInitClusterRole() {
    return initClusterRole;
  }

  public void setInitClusterRole(String initClusterRole) {
    this.initClusterRole = initClusterRole;
  }

  public ConfigOperator getOperator() {
    return operator;
  }

  public void setOperator(ConfigOperator operator) {
    this.operator = operator;
  }

  public ConfigRestapi getRestapi() {
    return restapi;
  }

  public void setRestapi(ConfigRestapi restapi) {
    this.restapi = restapi;
  }

  public ConfigWebConsole getAdminui() {
    return adminui;
  }

  public void setAdminui(ConfigWebConsole adminui) {
    this.adminui = adminui;
  }

  public ConfigJobs getJobs() {
    return jobs;
  }

  public void setJobs(ConfigJobs jobs) {
    this.jobs = jobs;
  }

  public ConfigDeploy getDeploy() {
    return deploy;
  }

  public void setDeploy(ConfigDeploy deploy) {
    this.deploy = deploy;
  }

  public ConfigAuthentication getAuthentication() {
    return authentication;
  }

  public void setAuthentication(ConfigAuthentication authentication) {
    this.authentication = authentication;
  }

  public ConfigPrometheus getPrometheus() {
    return prometheus;
  }

  public void setPrometheus(ConfigPrometheus prometheus) {
    this.prometheus = prometheus;
  }

  public ConfigGrafana getGrafana() {
    return grafana;
  }

  public void setGrafana(ConfigGrafana grafana) {
    this.grafana = grafana;
  }

  public ConfigExtensions getExtensions() {
    return extensions;
  }

  public void setExtensions(ConfigExtensions extensions) {
    this.extensions = extensions;
  }

  public ConfigDeveloper getDeveloper() {
    return developer;
  }

  public void setDeveloper(ConfigDeveloper developer) {
    this.developer = developer;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
