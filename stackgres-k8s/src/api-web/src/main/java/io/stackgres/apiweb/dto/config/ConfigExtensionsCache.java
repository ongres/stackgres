/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigExtensionsCache {

  private Boolean enabled;

  private List<String> preloadedExtensions;

  private ConfigExtensionsCachePersistentVolume persistentVolume;

  private String hostPath;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public List<String> getPreloadedExtensions() {
    return preloadedExtensions;
  }

  public void setPreloadedExtensions(List<String> preloadedExtensions) {
    this.preloadedExtensions = preloadedExtensions;
  }

  public ConfigExtensionsCachePersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(ConfigExtensionsCachePersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public String getHostPath() {
    return hostPath;
  }

  public void setHostPath(String hostPath) {
    this.hostPath = hostPath;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
