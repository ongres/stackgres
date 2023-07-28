/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigSpec {

  private String containerRegistry;

  private String imagePullPolicy;

  private StackGresConfigOptionalServiceAccount serviceAccount;

  private String initClusterRole;

  private StackGresConfigWebConsole adminui;

  private StackGresConfigJobs jobs;

  private StackGresConfigDeploy deploy;

  private StackGresConfigAuthentication authentication;

  private StackGresConfigPrometheus prometheus;

  private StackGresConfigGrafana grafana;

  private StackGresConfigExtensions extensions;

  private StackGresConfigDeveloper developer;

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

  public StackGresConfigOptionalServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(StackGresConfigOptionalServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
  }

  public String getInitClusterRole() {
    return initClusterRole;
  }

  public void setInitClusterRole(String initClusterRole) {
    this.initClusterRole = initClusterRole;
  }

  public StackGresConfigWebConsole getAdminui() {
    return adminui;
  }

  public void setAdminui(StackGresConfigWebConsole adminui) {
    this.adminui = adminui;
  }

  public StackGresConfigJobs getJobs() {
    return jobs;
  }

  public void setJobs(StackGresConfigJobs jobs) {
    this.jobs = jobs;
  }

  public StackGresConfigDeploy getDeploy() {
    return deploy;
  }

  public void setDeploy(StackGresConfigDeploy deploy) {
    this.deploy = deploy;
  }

  public StackGresConfigAuthentication getAuthentication() {
    return authentication;
  }

  public void setAuthentication(StackGresConfigAuthentication authentication) {
    this.authentication = authentication;
  }

  public StackGresConfigPrometheus getPrometheus() {
    return prometheus;
  }

  public void setPrometheus(StackGresConfigPrometheus prometheus) {
    this.prometheus = prometheus;
  }

  public StackGresConfigGrafana getGrafana() {
    return grafana;
  }

  public void setGrafana(StackGresConfigGrafana grafana) {
    this.grafana = grafana;
  }

  public StackGresConfigExtensions getExtensions() {
    return extensions;
  }

  public void setExtensions(StackGresConfigExtensions extensions) {
    this.extensions = extensions;
  }

  public StackGresConfigDeveloper getDeveloper() {
    return developer;
  }

  public void setDeveloper(StackGresConfigDeveloper developer) {
    this.developer = developer;
  }

  @Override
  public int hashCode() {
    return Objects.hash(adminui, authentication, containerRegistry, deploy, developer, extensions,
        grafana, imagePullPolicy, initClusterRole, jobs, prometheus, serviceAccount);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigSpec)) {
      return false;
    }
    StackGresConfigSpec other = (StackGresConfigSpec) obj;
    return Objects.equals(adminui, other.adminui)
        && Objects.equals(authentication, other.authentication)
        && Objects.equals(containerRegistry, other.containerRegistry)
        && Objects.equals(deploy, other.deploy)
        && Objects.equals(developer, other.developer)
        && Objects.equals(extensions, other.extensions)
        && Objects.equals(grafana, other.grafana)
        && Objects.equals(imagePullPolicy, other.imagePullPolicy)
        && Objects.equals(initClusterRole, other.initClusterRole)
        && Objects.equals(jobs, other.jobs)
        && Objects.equals(prometheus, other.prometheus)
        && Objects.equals(serviceAccount, other.serviceAccount);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
