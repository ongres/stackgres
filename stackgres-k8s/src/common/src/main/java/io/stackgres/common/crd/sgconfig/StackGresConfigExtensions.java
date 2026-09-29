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
import io.stackgres.common.AdditionalProperties;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresConfigExtensions extends AdditionalProperties {

  private List<String> repositoryUrls;

  private String refreshInterval;

  private Boolean refreshEnabled;

  private StackGresConfigExtensionsCache cache;

  public List<String> getRepositoryUrls() {
    return repositoryUrls;
  }

  public void setRepositoryUrls(List<String> repositoryUrls) {
    this.repositoryUrls = repositoryUrls;
  }

  public String getRefreshInterval() {
    return refreshInterval;
  }

  public void setRefreshInterval(String refreshInterval) {
    this.refreshInterval = refreshInterval;
  }

  public Boolean getRefreshEnabled() {
    return refreshEnabled;
  }

  public void setRefreshEnabled(Boolean refreshEnabled) {
    this.refreshEnabled = refreshEnabled;
  }

  public StackGresConfigExtensionsCache getCache() {
    return cache;
  }

  public void setCache(StackGresConfigExtensionsCache cache) {
    this.cache = cache;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cache, refreshEnabled, refreshInterval, repositoryUrls);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigExtensions)) {
      return false;
    }
    StackGresConfigExtensions other = (StackGresConfigExtensions) obj;
    return Objects.equals(cache, other.cache)
        && Objects.equals(refreshEnabled, other.refreshEnabled)
        && Objects.equals(refreshInterval, other.refreshInterval)
        && Objects.equals(repositoryUrls, other.repositoryUrls);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
