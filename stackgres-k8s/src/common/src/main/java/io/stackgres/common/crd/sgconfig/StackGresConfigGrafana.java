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
public class StackGresConfigGrafana {

  private Boolean autoEmbed;

  private String schema;

  private String webHost;

  private String datasourceName;

  private String user;

  private String password;

  private String secretNamespace;

  private String secretName;

  private String secretUserKey;

  private String secretPasswordKey;

  private String dashboardConfigMap;

  private String dashboardId;

  private String url;

  private String token;

  public Boolean getAutoEmbed() {
    return autoEmbed;
  }

  public void setAutoEmbed(Boolean autoEmbed) {
    this.autoEmbed = autoEmbed;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }

  public String getWebHost() {
    return webHost;
  }

  public void setWebHost(String webHost) {
    this.webHost = webHost;
  }

  public String getDatasourceName() {
    return datasourceName;
  }

  public void setDatasourceName(String datasourceName) {
    this.datasourceName = datasourceName;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSecretNamespace() {
    return secretNamespace;
  }

  public void setSecretNamespace(String secretNamespace) {
    this.secretNamespace = secretNamespace;
  }

  public String getSecretName() {
    return secretName;
  }

  public void setSecretName(String secretName) {
    this.secretName = secretName;
  }

  public String getSecretUserKey() {
    return secretUserKey;
  }

  public void setSecretUserKey(String secretUserKey) {
    this.secretUserKey = secretUserKey;
  }

  public String getSecretPasswordKey() {
    return secretPasswordKey;
  }

  public void setSecretPasswordKey(String secretPasswordKey) {
    this.secretPasswordKey = secretPasswordKey;
  }

  public String getDashboardConfigMap() {
    return dashboardConfigMap;
  }

  public void setDashboardConfigMap(String dashboardConfigMap) {
    this.dashboardConfigMap = dashboardConfigMap;
  }

  public String getDashboardId() {
    return dashboardId;
  }

  public void setDashboardId(String dashboardId) {
    this.dashboardId = dashboardId;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoEmbed, dashboardConfigMap, dashboardId, datasourceName, password,
        schema, secretName, secretNamespace, secretPasswordKey, secretUserKey, token, url, user,
        webHost);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigGrafana)) {
      return false;
    }
    StackGresConfigGrafana other = (StackGresConfigGrafana) obj;
    return Objects.equals(autoEmbed, other.autoEmbed)
        && Objects.equals(dashboardConfigMap, other.dashboardConfigMap)
        && Objects.equals(dashboardId, other.dashboardId)
        && Objects.equals(datasourceName, other.datasourceName)
        && Objects.equals(password, other.password) && Objects.equals(schema, other.schema)
        && Objects.equals(secretName, other.secretName)
        && Objects.equals(secretNamespace, other.secretNamespace)
        && Objects.equals(secretPasswordKey, other.secretPasswordKey)
        && Objects.equals(secretUserKey, other.secretUserKey) && Objects.equals(token, other.token)
        && Objects.equals(url, other.url)
        && Objects.equals(user, other.user) && Objects.equals(webHost, other.webHost);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
