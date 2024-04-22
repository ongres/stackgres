/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgshardedcluster;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.ValidEnum;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresShardedClusterShardingSphereMode {

  @ValidEnum(enumClass = StackGresShardingSphereModeType.class, allowNulls = false,
      message = "supported types are standalone and cluster")
  private String type;

  private Map<String, String> properties;

  private StackGresShardedClusterShardingSphereRepository repository;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public StackGresShardedClusterShardingSphereRepository getRepository() {
    return repository;
  }

  public void setRepository(StackGresShardedClusterShardingSphereRepository repository) {
    this.repository = repository;
  }

  @Override
  public int hashCode() {
    return Objects.hash(repository, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresShardedClusterShardingSphereMode)) {
      return false;
    }
    StackGresShardedClusterShardingSphereMode other = (StackGresShardedClusterShardingSphereMode) obj;
    return Objects.equals(repository, other.repository) && Objects.equals(type, other.type);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
