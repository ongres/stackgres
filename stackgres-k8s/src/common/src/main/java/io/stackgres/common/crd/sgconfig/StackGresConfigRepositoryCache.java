/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgconfig;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

/**
 * The cache of the StackGres images repository (the docir REST API) deployed by the Helm chart:
 * a StatefulSet that serves the catalog and the resolved images from local files, proxies the
 * requests not cached yet to the repository and copies the images into a local registry.
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigRepositoryCache {

  private Boolean enabled;

  private String refreshInterval;

  private List<String> preloadedImages;

  private Boolean pullImages;

  private String imageRegistry;

  private String registryImage;

  private Boolean offline;

  private StackGresConfigRepositoryCachePersistentVolume persistentVolume;

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

  public StackGresConfigRepositoryCachePersistentVolume getPersistentVolume() {
    return persistentVolume;
  }

  public void setPersistentVolume(StackGresConfigRepositoryCachePersistentVolume persistentVolume) {
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
  public int hashCode() {
    return Objects.hash(enabled, hostPath, imageRegistry, offline, persistentVolume,
        preloadedImages, pullImages, refreshInterval, registryImage, resources);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigRepositoryCache)) {
      return false;
    }
    StackGresConfigRepositoryCache other = (StackGresConfigRepositoryCache) obj;
    return Objects.equals(enabled, other.enabled)
        && Objects.equals(hostPath, other.hostPath)
        && Objects.equals(imageRegistry, other.imageRegistry)
        && Objects.equals(offline, other.offline)
        && Objects.equals(persistentVolume, other.persistentVolume)
        && Objects.equals(preloadedImages, other.preloadedImages)
        && Objects.equals(pullImages, other.pullImages)
        && Objects.equals(refreshInterval, other.refreshInterval)
        && Objects.equals(registryImage, other.registryImage)
        && Objects.equals(resources, other.resources);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
