/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class PodMetricsEndpoint {

  private String port;

  private String targetPort;

  private String path;

  private String scheme;

  private Map<String, String> params;

  private String interval;

  private String scrapeTimeout;

  private SafeTlsConfig tlsConfig;

  private SecretKeySelector bearerTokenSecret;

  private Boolean honorLabels;

  private Boolean honorTimestamps;

  private Boolean trackTimestampsStaleness;

  private BasicAuth basicAuth;

  private OAuth2 oauth2;

  private SafeAuthorization authorization;

  private List<RelabelConfig> metricRelabelings;

  private List<RelabelConfig> relabelings;

  private String proxyUrl;

  private Boolean followRedirects;

  private Boolean enableHttp2;

  private Boolean filterRunning;

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getTargetPort() {
    return targetPort;
  }

  public void setTargetPort(String targetPort) {
    this.targetPort = targetPort;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getScheme() {
    return scheme;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public void setParams(Map<String, String> params) {
    this.params = params;
  }

  public String getInterval() {
    return interval;
  }

  public void setInterval(String interval) {
    this.interval = interval;
  }

  public String getScrapeTimeout() {
    return scrapeTimeout;
  }

  public void setScrapeTimeout(String scrapeTimeout) {
    this.scrapeTimeout = scrapeTimeout;
  }

  public SafeTlsConfig getTlsConfig() {
    return tlsConfig;
  }

  public void setTlsConfig(SafeTlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
  }

  public SecretKeySelector getBearerTokenSecret() {
    return bearerTokenSecret;
  }

  public void setBearerTokenSecret(SecretKeySelector bearerTokenSecret) {
    this.bearerTokenSecret = bearerTokenSecret;
  }

  public Boolean getHonorLabels() {
    return honorLabels;
  }

  public void setHonorLabels(Boolean honorLabels) {
    this.honorLabels = honorLabels;
  }

  public Boolean getHonorTimestamps() {
    return honorTimestamps;
  }

  public void setHonorTimestamps(Boolean honorTimestamps) {
    this.honorTimestamps = honorTimestamps;
  }

  public Boolean getTrackTimestampsStaleness() {
    return trackTimestampsStaleness;
  }

  public void setTrackTimestampsStaleness(Boolean trackTimestampsStaleness) {
    this.trackTimestampsStaleness = trackTimestampsStaleness;
  }

  public BasicAuth getBasicAuth() {
    return basicAuth;
  }

  public void setBasicAuth(BasicAuth basicAuth) {
    this.basicAuth = basicAuth;
  }

  public OAuth2 getOauth2() {
    return oauth2;
  }

  public void setOauth2(OAuth2 oauth2) {
    this.oauth2 = oauth2;
  }

  public SafeAuthorization getAuthorization() {
    return authorization;
  }

  public void setAuthorization(SafeAuthorization authorization) {
    this.authorization = authorization;
  }

  public List<RelabelConfig> getMetricRelabelings() {
    return metricRelabelings;
  }

  public void setMetricRelabelings(List<RelabelConfig> metricRelabelings) {
    this.metricRelabelings = metricRelabelings;
  }

  public List<RelabelConfig> getRelabelings() {
    return relabelings;
  }

  public void setRelabelings(List<RelabelConfig> relabelings) {
    this.relabelings = relabelings;
  }

  public String getProxyUrl() {
    return proxyUrl;
  }

  public void setProxyUrl(String proxyUrl) {
    this.proxyUrl = proxyUrl;
  }

  public Boolean getFollowRedirects() {
    return followRedirects;
  }

  public void setFollowRedirects(Boolean followRedirects) {
    this.followRedirects = followRedirects;
  }

  public Boolean getEnableHttp2() {
    return enableHttp2;
  }

  public void setEnableHttp2(Boolean enableHttp2) {
    this.enableHttp2 = enableHttp2;
  }

  public Boolean getFilterRunning() {
    return filterRunning;
  }

  public void setFilterRunning(Boolean filterRunning) {
    this.filterRunning = filterRunning;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorization, basicAuth, bearerTokenSecret, enableHttp2, filterRunning,
        followRedirects, honorLabels, honorTimestamps, interval, metricRelabelings, oauth2, params,
        path, port, proxyUrl, relabelings, scheme, scrapeTimeout, targetPort, tlsConfig,
        trackTimestampsStaleness);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PodMetricsEndpoint)) {
      return false;
    }
    PodMetricsEndpoint other = (PodMetricsEndpoint) obj;
    return Objects.equals(authorization, other.authorization)
        && Objects.equals(basicAuth, other.basicAuth)
        && Objects.equals(bearerTokenSecret, other.bearerTokenSecret)
        && Objects.equals(enableHttp2, other.enableHttp2)
        && Objects.equals(filterRunning, other.filterRunning)
        && Objects.equals(followRedirects, other.followRedirects)
        && Objects.equals(honorLabels, other.honorLabels)
        && Objects.equals(honorTimestamps, other.honorTimestamps)
        && Objects.equals(interval, other.interval)
        && Objects.equals(metricRelabelings, other.metricRelabelings)
        && Objects.equals(oauth2, other.oauth2) && Objects.equals(params, other.params)
        && Objects.equals(path, other.path) && Objects.equals(port, other.port)
        && Objects.equals(proxyUrl, other.proxyUrl)
        && Objects.equals(relabelings, other.relabelings) && Objects.equals(scheme, other.scheme)
        && Objects.equals(scrapeTimeout, other.scrapeTimeout)
        && Objects.equals(targetPort, other.targetPort)
        && Objects.equals(tlsConfig, other.tlsConfig)
        && Objects.equals(trackTimestampsStaleness, other.trackTimestampsStaleness);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
