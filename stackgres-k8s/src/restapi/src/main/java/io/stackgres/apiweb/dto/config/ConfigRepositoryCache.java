/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ConfigRepositoryCache {

  private Boolean enabled;

  private String refreshInterval;

  private List<String> preloadedImages;

  private Boolean pullImages;

  private String imageRegistry;

  private String registryImage;

  private Boolean offline;

  private ConfigRepositoryCachePersistentVolume persistentVolume;

  private String hostPath;

  private ResourceRequirements resources;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(String refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  public List<String> getPreloadedImages() {
    return preloadedImages;
  }

  public void setPreloadedImages(List<String> preloadedImages) {
    this.preloadedImages = preloadedImages;
  }

  public Boolean getPullImages() {
    return pullImages;
  }

  public void setPullImages(Boolean pullImages) {
    this.pullImages = pullImages;
  }

  public String getImageRegistry() {
    return imageRegistry;
  }

  public void setImageRegistry(String imageRegistry) {
    this.imageRegistry = imageRegistry;
  }

  public String getRegistryImage() {
    return registryImage;
  }

  public void setRegistryImage(String registryImage) {
    this.registryImage = registryImage;
  }

  public Boolean getOffline() {
    return offline;
  }

  public void setOffline(Boolean offline) {
    this.offline = offline;
  }

  public ConfigRepositoryCachePersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(ConfigRepositoryCachePersistentVolume persistentVolume) {
    this.persistentVolume = persistentVolume;
  }

  public String getHostPath() {
    return hostPath;
  }

  public void setHostPath(String hostPath) {
    this.hostPath = hostPath;
  }

  public ResourceRequirements getResources() {
    return resources;
  }

  public void setResources(ResourceRequirements resources) {
    this.resources = resources;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
