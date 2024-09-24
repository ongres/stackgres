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
public class OAuth2 {

  private SecretOrConfigMap clientId;

  private SecretKeySelector clientSecret;

  private String tokenUrl;

  private List<String> scopes;

  private Map<String, String> endpointParams;

  private SafeTlsConfig tlsConfig;

  private String proxyUrl;

  private String noProxy;

  private Boolean proxyFromEnvironment;

  private SecretKeySelector proxyConnectHeader;

  public SecretOrConfigMap getClientId() {
    return clientId;
  }

  public void setClientId(SecretOrConfigMap clientId) {
    this.clientId = clientId;
  }

  public SecretKeySelector getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(SecretKeySelector clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getTokenUrl() {
    return tokenUrl;
  }

  public void setTokenUrl(String tokenUrl) {
    this.tokenUrl = tokenUrl;
  }

  public List<String> getScopes() {
    return scopes;
  }

  public void setScopes(List<String> scopes) {
    this.scopes = scopes;
  }

  public Map<String, String> getEndpointParams() {
    return endpointParams;
  }

  public void setEndpointParams(Map<String, String> endpointParams) {
    this.endpointParams = endpointParams;
  }

  public SafeTlsConfig getTlsConfig() {
    return tlsConfig;
  }

  public void setTlsConfig(SafeTlsConfig tlsConfig) {
    this.tlsConfig = tlsConfig;
  }

  public String getProxyUrl() {
    return proxyUrl;
  }

  public void setProxyUrl(String proxyUrl) {
    this.proxyUrl = proxyUrl;
  }

  public String getNoProxy() {
    return noProxy;
  }

  public void setNoProxy(String noProxy) {
    this.noProxy = noProxy;
  }

  public Boolean getProxyFromEnvironment() {
    return proxyFromEnvironment;
  }

  public void setProxyFromEnvironment(Boolean proxyFromEnvironment) {
    this.proxyFromEnvironment = proxyFromEnvironment;
  }

  public SecretKeySelector getProxyConnectHeader() {
    return proxyConnectHeader;
  }

  public void setProxyConnectHeader(SecretKeySelector proxyConnectHeader) {
    this.proxyConnectHeader = proxyConnectHeader;
  }

  @Override
  public int hashCode() {
    return Objects.hash(clientId, clientSecret, endpointParams, noProxy, proxyConnectHeader,
        proxyFromEnvironment, proxyUrl, scopes, tlsConfig, tokenUrl);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof OAuth2)) {
      return false;
    }
    OAuth2 other = (OAuth2) obj;
    return Objects.equals(clientId, other.clientId)
        && Objects.equals(clientSecret, other.clientSecret)
        && Objects.equals(endpointParams, other.endpointParams)
        && Objects.equals(noProxy, other.noProxy)
        && Objects.equals(proxyConnectHeader, other.proxyConnectHeader)
        && Objects.equals(proxyFromEnvironment, other.proxyFromEnvironment)
        && Objects.equals(proxyUrl, other.proxyUrl) && Objects.equals(scopes, other.scopes)
        && Objects.equals(tlsConfig, other.tlsConfig) && Objects.equals(tokenUrl, other.tokenUrl);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
