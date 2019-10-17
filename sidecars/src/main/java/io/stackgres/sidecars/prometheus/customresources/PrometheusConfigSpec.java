/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.prometheus.customresources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.SecurityContext;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PrometheusConfigSpec implements KubernetesResource {

  private Alerting alerting;

  private String baseImage;

  @JsonProperty("enableAdminAPI")
  private Boolean enableAdminApi;

  private String externalUrl;

  private Boolean listenLocal;

  private String logFormat;

  private String logLevel;

  private Boolean paused;

  private Integer replicas;

  private String retention;

  private String routePrefix;

  private LabelSelector ruleNamespaceSelector;

  private LabelSelector ruleSelector;

  private SecurityContext securityContext;

  private String serviceAccountName;

  private LabelSelector serviceMonitorNamespaceSelector;

  private LabelSelector serviceMonitorSelector;

  private String version;

  public Alerting getAlerting() {
    return alerting;
  }

  public void setAlerting(Alerting alerting) {
    this.alerting = alerting;
  }

  public String getBaseImage() {
    return baseImage;
  }

  public void setBaseImage(String baseImage) {
    this.baseImage = baseImage;
  }

  public Boolean getEnableAdminApi() {
    return enableAdminApi;
  }

  public void setEnableAdminApi(Boolean enableAdminApi) {
    this.enableAdminApi = enableAdminApi;
  }

  public String getExternalUrl() {
    return externalUrl;
  }

  public void setExternalUrl(String externalUrl) {
    this.externalUrl = externalUrl;
  }

  public Boolean getListenLocal() {
    return listenLocal;
  }

  public void setListenLocal(Boolean listenLocal) {
    this.listenLocal = listenLocal;
  }

  public String getLogFormat() {
    return logFormat;
  }

  public void setLogFormat(String logFormat) {
    this.logFormat = logFormat;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public Boolean getPaused() {
    return paused;
  }

  public void setPaused(Boolean paused) {
    this.paused = paused;
  }

  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }

  public String getRetention() {
    return retention;
  }

  public void setRetention(String retention) {
    this.retention = retention;
  }

  public String getRoutePrefix() {
    return routePrefix;
  }

  public void setRoutePrefix(String routePrefix) {
    this.routePrefix = routePrefix;
  }

  public LabelSelector getRuleNamespaceSelector() {
    return ruleNamespaceSelector;
  }

  public void setRuleNamespaceSelector(LabelSelector ruleNamespaceSelector) {
    this.ruleNamespaceSelector = ruleNamespaceSelector;
  }

  public LabelSelector getRuleSelector() {
    return ruleSelector;
  }

  public void setRuleSelector(LabelSelector ruleSelector) {
    this.ruleSelector = ruleSelector;
  }

  public SecurityContext getSecurityContext() {
    return securityContext;
  }

  public void setSecurityContext(SecurityContext securityContext) {
    this.securityContext = securityContext;
  }

  public String getServiceAccountName() {
    return serviceAccountName;
  }

  public void setServiceAccountName(String serviceAccountName) {
    this.serviceAccountName = serviceAccountName;
  }

  public LabelSelector getServiceMonitorNamespaceSelector() {
    return serviceMonitorNamespaceSelector;
  }

  public void setServiceMonitorNamespaceSelector(LabelSelector serviceMonitorNamespaceSelector) {
    this.serviceMonitorNamespaceSelector = serviceMonitorNamespaceSelector;
  }

  public LabelSelector getServiceMonitorSelector() {
    return serviceMonitorSelector;
  }

  public void setServiceMonitorSelector(LabelSelector serviceMonitorSelector) {
    this.serviceMonitorSelector = serviceMonitorSelector;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
