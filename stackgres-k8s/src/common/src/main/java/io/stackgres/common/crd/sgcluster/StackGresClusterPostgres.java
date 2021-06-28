/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterPostgres {

  @JsonProperty("ssl")
  @Valid
  private StackGresClusterSsl ssl;

  public StackGresClusterSsl getSsl() {
    return ssl;
  }

  public void setSsl(StackGresClusterSsl ssl) {
    this.ssl = ssl;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ssl);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPostgres)) {
      return false;
    }
    StackGresClusterPostgres other = (StackGresClusterPostgres) obj;
    return Objects.equals(ssl, other.ssl);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
