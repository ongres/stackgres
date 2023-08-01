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
public class ConfigExtensions {

  private List<String> repositoryUrls;

  private ConfigExtensionsCache cache;

  public List<String> getRepositoryUrls() {
    return repositoryUrls;
  }

  public void setRepositoryUrls(List<String> repositoryUrls) {
    this.repositoryUrls = repositoryUrls;
  }

  public ConfigExtensionsCache getCache() {
    return cache;
  }

  public void setCache(ConfigExtensionsCache cache) {
    this.cache = cache;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
