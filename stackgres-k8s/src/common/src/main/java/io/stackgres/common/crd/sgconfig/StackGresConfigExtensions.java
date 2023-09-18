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
public class StackGresConfigExtensions {

  private List<String> repositoryUrls;

  private StackGresConfigExtensionsCache cache;

  public List<String> getRepositoryUrls() {
    return repositoryUrls;
  }

  public void setRepositoryUrls(List<String> repositoryUrls) {
    this.repositoryUrls = repositoryUrls;
  }

  public StackGresConfigExtensionsCache getCache() {
    return cache;
  }

  public void setCache(StackGresConfigExtensionsCache cache) {
    this.cache = cache;
  }

  @Override
  public int hashCode() {
    return Objects.hash(cache, repositoryUrls);
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
        && Objects.equals(repositoryUrls, other.repositoryUrls);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
