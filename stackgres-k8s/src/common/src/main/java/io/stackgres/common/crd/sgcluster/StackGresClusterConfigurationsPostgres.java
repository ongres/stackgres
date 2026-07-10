/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigSpec;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterConfigurationsPostgres extends StackGresPostgresConfigSpec {

  private String walPath;

  public String getWalPath() {
    return walPath;
  }

  public void setWalPath(String walPath) {
    this.walPath = walPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), walPath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterConfigurationsPostgres)) {
      return false;
    }
    StackGresClusterConfigurationsPostgres other = (StackGresClusterConfigurationsPostgres) obj;
    return super.equals(other)
        && Objects.equals(walPath, other.walPath);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
