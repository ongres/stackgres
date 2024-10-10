/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.OsDetector;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.docir.model.AddonVersion;
import io.stackgres.common.docir.model.PlatformWithRevision;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DocirAddonVersion {

  private String version;

  private String os;

  private String arch;

  private String baseName;

  private Integer baseMajor;

  private Integer baseMinor;

  private String revision;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getOs() {
    return os;
  }

  @JsonIgnore
  public String getOsOrDefault() {
    return Optional.ofNullable(os).orElse(DocirUtil.DEFAULT_OS);
  }

  public void setOs(String os) {
    this.os = os;
  }

  public String getArch() {
    return arch;
  }

  @JsonIgnore
  public String getArchOrDefault() {
    return Optional.ofNullable(arch)
        .map(OsDetector::normalizeArch)
        .orElse(DocirUtil.DEFAULT_ARCH);
  }

  public void setArch(String arch) {
    this.arch = arch;
  }

  public String getBaseName() {
    return baseName;
  }

  public void setBaseName(String baseName) {
    this.baseName = baseName;
  }

  public Integer getBaseMajor() {
    return baseMajor;
  }

  public void setBaseMajor(Integer baseMajor) {
    this.baseMajor = baseMajor;
  }

  public Integer getBaseMinor() {
    return baseMinor;
  }

  public void setBaseMinor(Integer baseMinor) {
    this.baseMinor = baseMinor;
  }

  @JsonIgnore
  public String getBaseIdentity() {
    return DocirUtil.getBaseIdentity(baseName, baseMajor, baseMinor);
  }

  public String getRevision() {
    return revision;
  }

  public void setRevision(String revision) {
    this.revision = revision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(revision, baseName, baseMajor, baseMinor, version, os, arch);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirAddonVersion)) {
      return false;
    }
    DocirAddonVersion other = (DocirAddonVersion) obj;
    return Objects.equals(revision, other.revision)
        && Objects.equals(baseName, other.baseName)
        && Objects.equals(baseMajor, other.baseMajor)
        && Objects.equals(baseMinor, other.baseMinor)
        && Objects.equals(version, other.version)
        && Objects.equals(os, other.os)
        && Objects.equals(arch, other.arch);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  public static DocirAddonVersion fromMetadata(
      AddonVersion addonVersion,
      PlatformWithRevision platform) {
    return new DocirAddonVersionBuilder()
        .withVersion(addonVersion.version())
        .withBaseName(platform.base())
        .withBaseMajor(DocirUtil.parseIntegerOrNull(platform.baseMajor()))
        .withBaseMinor(DocirUtil.parseIntegerOrNull(platform.baseMinor()))
        .withRevision(platform.revision())
        .withOs(platform.os())
        .withArch(platform.architecture())
        .build();
  }

}
