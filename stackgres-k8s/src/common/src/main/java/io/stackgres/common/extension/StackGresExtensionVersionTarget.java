/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.Objects;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.crd.sgcluster.StackGresPostgresFlavor;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresExtensionVersionTarget {

  private String flavor;

  @NotNull(message = "postgresVersion is required")
  private String postgresVersion;

  private String build;

  private String arch;

  private String os;

  public String getFlavor() {
    return flavor;
  }

  @JsonIgnore
  public String getFlavorOrDefault() {
    return Optional.ofNullable(flavor)
        .filter(flavor -> !flavor.equals(StackGresPostgresFlavor.VANILLA.toString()))
        .orElse(null);
  }

  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getBuild() {
    return build;
  }

  public void setBuild(String build) {
    this.build = build;
  }

  public String getArch() {
    return arch;
  }

  @JsonIgnore
  public String getArchOrDefault() {
    return Optional.ofNullable(arch).orElse(ExtensionUtil.DEFAULT_ARCH);
  }

  public void setArch(String arch) {
    this.arch = arch;
  }

  public String getOs() {
    return os;
  }

  @JsonIgnore
  public String getOsOrDefault() {
    return Optional.ofNullable(os).orElse(ExtensionUtil.DEFAULT_OS);
  }

  public void setOs(String os) {
    this.os = os;
  }

  @Override
  public int hashCode() {
    return Objects.hash(arch, build, flavor, os, postgresVersion);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionVersionTarget)) {
      return false;
    }
    StackGresExtensionVersionTarget other = (StackGresExtensionVersionTarget) obj;
    return Objects.equals(arch, other.arch) && Objects.equals(build, other.build)
        && Objects.equals(flavor, other.flavor) && Objects.equals(os, other.os)
        && Objects.equals(postgresVersion, other.postgresVersion);
  }

}
