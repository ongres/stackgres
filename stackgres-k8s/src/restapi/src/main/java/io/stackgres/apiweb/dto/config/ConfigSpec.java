/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.LocalObjectReference;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigSpec {

  private String containerRegistry;

  private String imagePullPolicy;

  private List<LocalObjectReference> imagePullSecrets;

  private List<String> allowedNamespaces;

  private Map<String, String> allowedNamespaceLabelSelector;

  private Boolean disableClusterRole;

  private Boolean disableCrdsAndWebhooksUpdate;

  private Boolean allowImpersonationForRestApi;

  private String sgConfigNamespace;

  private ConfigOptionalServiceAccount serviceAccount;

  private ConfigOperator operator;

  private ConfigRestapi restapi;

  private ConfigAdminui adminui;

  private ConfigCollector collector;

  private ConfigJobs jobs;

  private ConfigDeploy deploy;

  private ConfigCert cert;

  private ConfigAuthentication authentication;

  private ConfigPrometheus prometheus;

  private ConfigGrafana grafana;

  private ConfigExtensions extensions;

  private ConfigDeveloper developer;

  private ConfigShardingSphere shardingSphere;

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

  public List<LocalObjectReference> getImagePullSecrets() {
    return imagePullSecrets;
  }

  public void setImagePullSecrets(List<LocalObjectReference> imagePullSecrets) {
    this.imagePullSecrets = imagePullSecrets;
  }

  public List<String> getAllowedNamespaces() {
    return allowedNamespaces;
  }

  public void setAllowedNamespaces(List<String> allowedNamespaces) {
    this.allowedNamespaces = allowedNamespaces;
  }

  public Map<String, String> getAllowedNamespaceLabelSelector() {
    return allowedNamespaceLabelSelector;
  }

  public void setAllowedNamespaceLabelSelector(Map<String, String> allowedNamespaceLabelSelector) {
    this.allowedNamespaceLabelSelector = allowedNamespaceLabelSelector;
  }

  public Boolean getDisableClusterRole() {
    return disableClusterRole;
  }

  public void setDisableClusterRole(Boolean disableClusterRole) {
    this.disableClusterRole = disableClusterRole;
  }

  public Boolean getDisableCrdsAndWebhooksUpdate() {
    return disableCrdsAndWebhooksUpdate;
  }

  public void setDisableCrdsAndWebhooksUpdate(Boolean disableCrdsAndWebhooksUpdate) {
    this.disableCrdsAndWebhooksUpdate = disableCrdsAndWebhooksUpdate;
  }

  public Boolean getAllowImpersonationForRestApi() {
    return allowImpersonationForRestApi;
  }

  public void setAllowImpersonationForRestApi(Boolean allowImpersonationForRestApi) {
    this.allowImpersonationForRestApi = allowImpersonationForRestApi;
  }

  public String getSgConfigNamespace() {
    return sgConfigNamespace;
  }

  public void setSgConfigNamespace(String sgConfigNamespace) {
    this.sgConfigNamespace = sgConfigNamespace;
  }

  public ConfigOptionalServiceAccount getServiceAccount() {
    return serviceAccount;
  }

  public void setServiceAccount(ConfigOptionalServiceAccount serviceAccount) {
    this.serviceAccount = serviceAccount;
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

  public ConfigAdminui getAdminui() {
    return adminui;
  }

  public void setAdminui(ConfigAdminui adminui) {
    this.adminui = adminui;
  }

  public ConfigCollector getCollector() {
    return collector;
  }

  public void setCollector(ConfigCollector collector) {
    this.collector = collector;
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

  public ConfigCert getCert() {
    return cert;
  }

  public void setCert(ConfigCert cert) {
    this.cert = cert;
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

  public ConfigShardingSphere getShardingSphere() {
    return shardingSphere;
  }

  public void setShardingSphere(ConfigShardingSphere shardingSphere) {
    this.shardingSphere = shardingSphere;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
