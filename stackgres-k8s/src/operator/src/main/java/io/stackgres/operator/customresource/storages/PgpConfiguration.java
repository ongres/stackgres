/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.storages;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class PgpConfiguration {

  @JsonProperty("key")
  @NotNull(message = "The key is required")
  private SecretKeySelector key;

  public SecretKeySelector getKey() {
    return key;
  }

  public void setKey(SecretKeySelector key) {
    this.key = key;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof PgpConfiguration)) {
      return false;
    }
    PgpConfiguration other = (PgpConfiguration) obj;
    return Objects.equals(key, other.key);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("key", key)
        .toString();
  }

}
