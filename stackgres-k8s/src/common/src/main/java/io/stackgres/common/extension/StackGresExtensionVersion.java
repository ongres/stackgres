/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresExtensionVersion {

  @NotNull(message = "version is required")
  private String version;

  @NotEmpty(message = "availableFor is required and must not be empty")
  @Valid
  private List<StackGresExtensionVersionTarget> availableFor;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<StackGresExtensionVersionTarget> getAvailableFor() {
    return availableFor;
  }

  public void setAvailableFor(List<StackGresExtensionVersionTarget> availableFor) {
    this.availableFor = availableFor;
  }

  @Override
  public int hashCode() {
    return Objects.hash(version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionVersion)) {
      return false;
    }
    StackGresExtensionVersion other = (StackGresExtensionVersion) obj;
    return Objects.equals(version, other.version);
  }

}
