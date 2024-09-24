/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.prometheus;

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
public class SafeTlsConfig {

  private SecretOrConfigMap ca;

  private SecretOrConfigMap cert;

  private SecretKeySelector keySecret;

  private String serverName;

  private Boolean insecureSkipVerify;

  private String minVersion;

  private String maxVersion;

  public SecretOrConfigMap getCa() {
    return ca;
  }

  public void setCa(SecretOrConfigMap ca) {
    this.ca = ca;
  }

  public SecretOrConfigMap getCert() {
    return cert;
  }

  public void setCert(SecretOrConfigMap cert) {
    this.cert = cert;
  }

  public SecretKeySelector getKeySecret() {
    return keySecret;
  }

  public void setKeySecret(SecretKeySelector keySecret) {
    this.keySecret = keySecret;
  }

  public String getServerName() {
    return serverName;
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  public Boolean getInsecureSkipVerify() {
    return insecureSkipVerify;
  }

  public void setInsecureSkipVerify(Boolean insecureSkipVerify) {
    this.insecureSkipVerify = insecureSkipVerify;
  }

  public String getMinVersion() {
    return minVersion;
  }

  public void setMinVersion(String minVersion) {
    this.minVersion = minVersion;
  }

  public String getMaxVersion() {
    return maxVersion;
  }

  public void setMaxVersion(String maxVersion) {
    this.maxVersion = maxVersion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ca, cert, insecureSkipVerify, keySecret, maxVersion, minVersion,
        serverName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SafeTlsConfig)) {
      return false;
    }
    SafeTlsConfig other = (SafeTlsConfig) obj;
    return Objects.equals(ca, other.ca) && Objects.equals(cert, other.cert)
        && Objects.equals(insecureSkipVerify, other.insecureSkipVerify)
        && Objects.equals(keySecret, other.keySecret)
        && Objects.equals(maxVersion, other.maxVersion)
        && Objects.equals(minVersion, other.minVersion)
        && Objects.equals(serverName, other.serverName);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
