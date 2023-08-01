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

  private Boolean showStackTraces;

  private Boolean useJvmImages;

  private Boolean enableJvmDebug;

  private Boolean enableJvmDebugSuspend;

  private Boolean allowPullExtensionsFromImageRepository;

  private Boolean disableArbitraryUser;

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

  @Override
  public int hashCode() {
    return Objects.hash(allowPullExtensionsFromImageRepository, disableArbitraryUser,
        enableJvmDebug, enableJvmDebugSuspend, logLevel, showStackTraces, useJvmImages, version);
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
        && Objects.equals(logLevel, other.logLevel)
        && Objects.equals(showStackTraces, other.showStackTraces)
        && Objects.equals(useJvmImages, other.useJvmImages)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
