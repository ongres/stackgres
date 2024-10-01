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
public class StackGresConfigCert {

  private Boolean autoapprove;

  private Boolean createForOperator;

  private Boolean createForWebApi;

  private Boolean createForCollector;

  private Boolean resetCerts;

  private String secretName;

  private Boolean regenerateCert;

  private Integer certDuration;

  private String webSecretName;

  private Boolean regenerateWebCert;

  private Boolean regenerateWebRsa;

  private Integer webCertDuration;

  private Integer webRsaDuration;

  private String collectorSecretName;

  private Boolean regenerateCollectorCert;

  private Integer collectorCertDuration;

  private StackGresConfigCertManager certManager;

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

  public Boolean getCreateForCollector() {
    return createForCollector;
  }

  public void setCreateForCollector(Boolean createForCollector) {
    this.createForCollector = createForCollector;
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

  public String getCollectorSecretName() {
    return collectorSecretName;
  }

  public void setCollectorSecretName(String collectorSecretName) {
    this.collectorSecretName = collectorSecretName;
  }

  public Boolean getRegenerateCollectorCert() {
    return regenerateCollectorCert;
  }

  public void setRegenerateCollectorCert(Boolean regenerateCollectorCert) {
    this.regenerateCollectorCert = regenerateCollectorCert;
  }

  public Integer getCollectorCertDuration() {
    return collectorCertDuration;
  }

  public void setCollectorCertDuration(Integer collectorCertDuration) {
    this.collectorCertDuration = collectorCertDuration;
  }

  public StackGresConfigCertManager getCertManager() {
    return certManager;
  }

  public void setCertManager(StackGresConfigCertManager certManager) {
    this.certManager = certManager;
  }

  @Override
  public int hashCode() {
    return Objects.hash(autoapprove, certDuration, certManager, collectorCertDuration,
        collectorSecretName, createForCollector, createForOperator, createForWebApi, regenerateCert,
        regenerateCollectorCert, regenerateWebCert, regenerateWebRsa, resetCerts, secretName,
        webCertDuration, webRsaDuration, webSecretName);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCert)) {
      return false;
    }
    StackGresConfigCert other = (StackGresConfigCert) obj;
    return Objects.equals(autoapprove, other.autoapprove)
        && Objects.equals(certDuration, other.certDuration)
        && Objects.equals(certManager, other.certManager)
        && Objects.equals(collectorCertDuration, other.collectorCertDuration)
        && Objects.equals(collectorSecretName, other.collectorSecretName)
        && Objects.equals(createForCollector, other.createForCollector)
        && Objects.equals(createForOperator, other.createForOperator)
        && Objects.equals(createForWebApi, other.createForWebApi)
        && Objects.equals(regenerateCert, other.regenerateCert)
        && Objects.equals(regenerateCollectorCert, other.regenerateCollectorCert)
        && Objects.equals(regenerateWebCert, other.regenerateWebCert)
        && Objects.equals(regenerateWebRsa, other.regenerateWebRsa)
        && Objects.equals(resetCerts, other.resetCerts)
        && Objects.equals(secretName, other.secretName)
        && Objects.equals(webCertDuration, other.webCertDuration)
        && Objects.equals(webRsaDuration, other.webRsaDuration)
        && Objects.equals(webSecretName, other.webSecretName);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
