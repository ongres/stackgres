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
public class StackGresConfigCollectorScalingDeploymentSgCluster {

  private String namespace;

  private String name;

  private List<Integer> indexes;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<Integer> getIndexes() {
    return indexes;
  }

  public void setIndexes(List<Integer> indexes) {
    this.indexes = indexes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(indexes, name, namespace);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresConfigCollectorScalingDeploymentSgCluster)) {
      return false;
    }
    StackGresConfigCollectorScalingDeploymentSgCluster other = (StackGresConfigCollectorScalingDeploymentSgCluster) obj;
    return Objects.equals(indexes, other.indexes) && Objects.equals(name, other.name)
        && Objects.equals(namespace, other.namespace);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
