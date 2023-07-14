/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresExtensionVersion {

  @NotNull(message = "version is required")
  private String version;

  private List<String> extraMounts;

  @NotEmpty(message = "availableFor is required and must not be empty")
  @Valid
  private List<StackGresExtensionVersionTarget> availableFor;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<String> getExtraMounts() {
    return extraMounts;
  }

  public void setExtraMounts(List<String> extraMounts) {
    this.extraMounts = extraMounts;
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
