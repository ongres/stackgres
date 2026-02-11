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
public class StackGresConfigDeveloper {

  private String version;

  private String logLevel;

  private Boolean showDebug;

  private Boolean showStackTraces;

  private Boolean useJvmImages;

  private Boolean enableJvmDebug;

  private Boolean enableJvmDebugSuspend;

  private String externalOperatorIp;

  private Integer externalOperatorPort;

  private String externalRestApiIp;

  private Integer externalRestApiPort;

  private Boolean allowPullExtensionsFromImageRepository;

  private Boolean disableArbitraryUser;

  private StackGresConfigDeveloperPatches patches;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public Boolean getShowDebug() {
    return showDebug;
  }

  public void setShowDebug(Boolean showDebug) {
    this.showDebug = showDebug;
  }

  public Boolean getShowStackTraces() {
    return showStackTraces;
  }

  public void setShowStackTraces(Boolean showStackTraces) {
    this.showStackTraces = showStackTraces;
  }

  public Boolean getUseJvmImages() {
    return useJvmImages;
  }

  public void setUseJvmImages(Boolean useJvmImages) {
    this.useJvmImages = useJvmImages;
  }

  public Boolean getEnableJvmDebug() {
    return enableJvmDebug;
  }

  public void setEnableJvmDebug(Boolean enableJvmDebug) {
    this.enableJvmDebug = enableJvmDebug;
  }

  public Boolean getEnableJvmDebugSuspend() {
    return enableJvmDebugSuspend;
  }

  public void setEnableJvmDebugSuspend(Boolean enableJvmDebugSuspend) {
    this.enableJvmDebugSuspend = enableJvmDebugSuspend;
  }

  public String getExternalOperatorIp() {
    return externalOperatorIp;
  }

  public void setExternalOperatorIp(String externalOperatorIp) {
    this.externalOperatorIp = externalOperatorIp;
  }

  public Integer getExternalOperatorPort() {
    return externalOperatorPort;
  }

  public void setExternalOperatorPort(Integer externalOperatorPort) {
    this.externalOperatorPort = externalOperatorPort;
  }

  public String getExternalRestApiIp() {
    return externalRestApiIp;
  }

  public void setExternalRestApiIp(String externalRestApiIp) {
    this.externalRestApiIp = externalRestApiIp;
  }

  public Integer getExternalRestApiPort() {
    return externalRestApiPort;
  }

  public void setExternalRestApiPort(Integer externalRestApiPort) {
    this.externalRestApiPort = externalRestApiPort;
  }

  public Boolean getAllowPullExtensionsFromImageRepository() {
    return allowPullExtensionsFromImageRepository;
  }

  public void setAllowPullExtensionsFromImageRepository(
      Boolean allowPullExtensionsFromImageRepository) {
    this.allowPullExtensionsFromImageRepository = allowPullExtensionsFromImageRepository;
  }

  public Boolean getDisableArbitraryUser() {
    return disableArbitraryUser;
  }

  public void setDisableArbitraryUser(Boolean disableArbitraryUser) {
    this.disableArbitraryUser = disableArbitraryUser;
  }

  public StackGresConfigDeveloperPatches getPatches() {
    return patches;
  }

  public void setPatches(StackGresConfigDeveloperPatches patches) {
    this.patches = patches;
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowPullExtensionsFromImageRepository, disableArbitraryUser,
        enableJvmDebug, enableJvmDebugSuspend, externalOperatorIp, externalOperatorPort,
        externalRestApiIp, externalRestApiPort, logLevel, patches, showDebug, showStackTraces,
        useJvmImages, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigDeveloper)) {
      return false;
    }
    StackGresConfigDeveloper other = (StackGresConfigDeveloper) obj;
    return Objects.equals(allowPullExtensionsFromImageRepository,
        other.allowPullExtensionsFromImageRepository)
        && Objects.equals(disableArbitraryUser, other.disableArbitraryUser)
        && Objects.equals(enableJvmDebug, other.enableJvmDebug)
        && Objects.equals(enableJvmDebugSuspend, other.enableJvmDebugSuspend)
        && Objects.equals(externalOperatorIp, other.externalOperatorIp)
        && Objects.equals(externalOperatorPort, other.externalOperatorPort)
        && Objects.equals(externalRestApiIp, other.externalRestApiIp)
        && Objects.equals(externalRestApiPort, other.externalRestApiPort)
        && Objects.equals(logLevel, other.logLevel) && Objects.equals(patches, other.patches)
        && Objects.equals(showDebug, other.showDebug)
        && Objects.equals(showStackTraces, other.showStackTraces)
        && Objects.equals(useJvmImages, other.useJvmImages)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
