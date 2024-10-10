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
import com.google.common.base.Predicates;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.OsDetector;
import io.stackgres.common.docir.model.ExtensionVersion;
import io.stackgres.common.docir.model.PlatformWithRevision;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class DocirExtensionVersion {

  private String version;

  private String flavor;

  private String flavorVersion;

  private String baseName;

  private Integer baseMajor;

  private Integer baseMinor;

  private String revision;

  private String arch;

  private String os;

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getFlavor() {
    return flavor;
  }

  @JsonIgnore
  public String getFlavorOrDefault() {
    return Optional.ofNullable(flavor)
        .orElse(DocirUtil.DEFAULT_FLAVOR);
  }

  @JsonIgnore
  public String getFlavorOrNullIfDefault() {
    return Optional.ofNullable(flavor)
        .filter(Predicates.not(DocirUtil.DEFAULT_FLAVOR::equals))
        .orElse(null);
  }

  public void setFlavor(String flavor) {
    this.flavor = flavor;
  }

  public String getFlavorVersion() {
    return flavorVersion;
  }

  public void setFlavorVersion(String flavorVersion) {
    this.flavorVersion = flavorVersion;
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

  @Override
  public int hashCode() {
    return Objects.hash(getArchOrDefault(), getFlavorOrDefault(), getOsOrDefault(), flavorVersion,
        revision, baseName, baseMajor, baseMinor, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirExtensionVersion)) {
      return false;
    }
    DocirExtensionVersion other = (DocirExtensionVersion) obj;
    return Objects.equals(getArchOrDefault(), other.getArchOrDefault())
        && Objects.equals(getFlavorOrDefault(), other.getFlavorOrDefault())
        && Objects.equals(getOsOrDefault(), other.getOsOrDefault())
        && Objects.equals(flavorVersion, other.flavorVersion)
        && Objects.equals(revision, other.revision)
        && Objects.equals(baseName, other.baseName)
        && Objects.equals(baseMajor, other.baseMajor)
        && Objects.equals(baseMinor, other.baseMinor)
        && Objects.equals(version, other.version);
  }

  public static DocirExtensionVersion fromMetadata(
      String flavor,
      String flavorVersion,
      ExtensionVersion extensionVersion,
      PlatformWithRevision platform) {
    return new DocirExtensionVersionBuilder()
        .withFlavor(flavor)
        .withFlavorVersion(flavorVersion)
        .withVersion(extensionVersion.version())
        .withBaseName(platform.base())
        .withBaseMajor(DocirUtil.parseIntegerOrNull(platform.baseMajor()))
        .withBaseMinor(DocirUtil.parseIntegerOrNull(platform.baseMinor()))
        .withRevision(platform.revision())
        .withOs(platform.os())
        .withArch(platform.architecture())
        .build();
  }

}
