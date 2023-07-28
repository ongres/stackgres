/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.List;
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
public class StackGresConfigExtensionsCache {

  private Boolean enabled;

  private List<String> preloadedExtensions;

  private StackGresConfigExtensionsCachePersistentVolume persistentVolume;

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

  public StackGresConfigExtensionsCachePersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresConfigExtensionsCachePersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public String getHostPath() {
    return hostPath;
  }

  public void setHostPath(String hostPath) {
    this.hostPath = hostPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, hostPath, persistentVolume, preloadedExtensions);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigExtensionsCache)) {
      return false;
    }
    StackGresConfigExtensionsCache other = (StackGresConfigExtensionsCache) obj;
    return Objects.equals(enabled, other.enabled) && Objects.equals(hostPath, other.hostPath)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(preloadedExtensions, other.preloadedExtensions);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
