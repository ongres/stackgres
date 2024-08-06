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
public class ConfigCert {

  private Boolean autoapprove;

  private Boolean createForOperator;

  private Boolean createForWebApi;

  private Boolean resetCerts;

  private String secretName;

  private Boolean regenerateCert;

  private Integer certDuration;

  private String webSecretName;

  private Boolean regenerateWebCert;

  private Boolean regenerateWebRsa;

  private Integer webCertDuration;

  private Integer webRsaDuration;

  private ConfigCertManager certManager;

  public Boolean getAutoapprove() {
    return autoapprove;
  }

  public void setAutoapprove(Boolean autoapprove) {
    this.autoapprove = autoapprove;
  }

  public Boolean getCreateForOperator() {
    return createForOperator;
  }

  public void setCreateForOperator(Boolean createForOperator) {
    this.createForOperator = createForOperator;
  }

  public Boolean getCreateForWebApi() {
    return createForWebApi;
  }

  public void setCreateForWebApi(Boolean createForWebApi) {
    this.createForWebApi = createForWebApi;
  }

  public Boolean getResetCerts() {
    return resetCerts;
  }

  public void setResetCerts(Boolean resetCerts) {
    this.resetCerts = resetCerts;
  }

  public String getSecretName() {
    return secretName;
  }

  public void setSecretName(String secretName) {
    this.secretName = secretName;
  }

  public Boolean getRegenerateCert() {
    return regenerateCert;
  }

  public void setRegenerateCert(Boolean regenerateCert) {
    this.regenerateCert = regenerateCert;
  }

  public Integer getCertDuration() {
    return certDuration;
  }

  public void setCertDuration(Integer certDuration) {
    this.certDuration = certDuration;
  }

  public String getWebSecretName() {
    return webSecretName;
  }

  public void setWebSecretName(String webSecretName) {
    this.webSecretName = webSecretName;
  }

  public Boolean getRegenerateWebCert() {
    return regenerateWebCert;
  }

  public void setRegenerateWebCert(Boolean regenerateWebCert) {
    this.regenerateWebCert = regenerateWebCert;
  }

  public Boolean getRegenerateWebRsa() {
    return regenerateWebRsa;
  }

  public void setRegenerateWebRsa(Boolean regenerateWebRsa) {
    this.regenerateWebRsa = regenerateWebRsa;
  }

  public Integer getWebCertDuration() {
    return webCertDuration;
  }

  public void setWebCertDuration(Integer webCertDuration) {
    this.webCertDuration = webCertDuration;
  }

  public Integer getWebRsaDuration() {
    return webRsaDuration;
  }

  public void setWebRsaDuration(Integer webRsaDuration) {
    this.webRsaDuration = webRsaDuration;
  }

  public ConfigCertManager getCertManager() {
    return certManager;
  }

  public void setCertManager(ConfigCertManager certManager) {
    this.certManager = certManager;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
