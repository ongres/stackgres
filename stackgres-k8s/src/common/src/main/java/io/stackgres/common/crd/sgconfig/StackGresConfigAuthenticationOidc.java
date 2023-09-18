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
import io.stackgres.common.crd.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigAuthenticationOidc {

  private String tlsVerification;

  private String authServerUrl;

  private String clientId;

  private String credentialsSecret;

  private SecretKeySelector clientIdSecretRef;

  private SecretKeySelector credentialsSecretSecretRef;

  public String getTlsVerification() {
    return tlsVerification;
  }

  public void setTlsVerification(String tlsVerification) {
    this.tlsVerification = tlsVerification;
  }

  public String getAuthServerUrl() {
    return authServerUrl;
  }

  public void setAuthServerUrl(String authServerUrl) {
    this.authServerUrl = authServerUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getCredentialsSecret() {
    return credentialsSecret;
  }

  public void setCredentialsSecret(String credentialsSecret) {
    this.credentialsSecret = credentialsSecret;
  }

  public SecretKeySelector getClientIdSecretRef() {
    return clientIdSecretRef;
  }

  public void setClientIdSecretRef(SecretKeySelector clientIdSecretRef) {
    this.clientIdSecretRef = clientIdSecretRef;
  }

  public SecretKeySelector getCredentialsSecretSecretRef() {
    return credentialsSecretSecretRef;
  }

  public void setCredentialsSecretSecretRef(SecretKeySelector credentialsSecretSecretRef) {
    this.credentialsSecretSecretRef = credentialsSecretSecretRef;
  }

  @Override
  public int hashCode() {
    return Objects.hash(authServerUrl, clientId, clientIdSecretRef, credentialsSecret,
        credentialsSecretSecretRef, tlsVerification);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigAuthenticationOidc)) {
      return false;
    }
    StackGresConfigAuthenticationOidc other = (StackGresConfigAuthenticationOidc) obj;
    return Objects.equals(authServerUrl, other.authServerUrl)
        && Objects.equals(clientId, other.clientId)
        && Objects.equals(clientIdSecretRef, other.clientIdSecretRef)
        && Objects.equals(credentialsSecret, other.credentialsSecret)
        && Objects.equals(credentialsSecretSecretRef, other.credentialsSecretSecretRef)
        && Objects.equals(tlsVerification, other.tlsVerification);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
