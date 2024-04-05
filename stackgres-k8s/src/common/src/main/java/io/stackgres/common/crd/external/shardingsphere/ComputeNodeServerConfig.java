/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.external.shardingsphere;

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
public class ComputeNodeServerConfig extends ComputeNodeProps {

  private ComputeNodeAuthority authority;

  private ComputeNodeMode mode;

  public ComputeNodeAuthority getAuthority() {
    return authority;
  }

  public void setAuthority(ComputeNodeAuthority authority) {
    this.authority = authority;
  }

  public ComputeNodeMode getMode() {
    return mode;
  }

  public void setMode(ComputeNodeMode mode) {
    this.mode = mode;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(authority, mode);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof ComputeNodeServerConfig)) {
      return false;
    }
    ComputeNodeServerConfig other = (ComputeNodeServerConfig) obj;
    return Objects.equals(authority, other.authority) && Objects.equals(mode, other.mode);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
