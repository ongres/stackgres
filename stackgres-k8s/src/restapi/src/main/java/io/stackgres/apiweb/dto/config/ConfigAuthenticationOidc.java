/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigAuthenticationOidc {

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
