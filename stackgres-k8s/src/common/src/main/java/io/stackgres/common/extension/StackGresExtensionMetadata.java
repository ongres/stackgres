/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public class StackGresExtensionMetadata
    implements Comparable<StackGresExtensionMetadata> {

  private final StackGresExtension extension;
  private final StackGresExtensionVersion version;
  private final StackGresExtensionVersionTarget target;
  private final StackGresExtensionPublisher publisher;
  private final StackGresExtensionMetadataBuild build;

  public StackGresExtensionMetadata(StackGresExtension extension,
      StackGresExtensionVersion version, StackGresExtensionVersionTarget target,
      StackGresExtensionPublisher publisher) {
    super();
    this.extension = extension;
    this.version = version;
    this.target = target;
    this.publisher = publisher;
    this.build = new StackGresExtensionMetadataBuild(target.getBuild());
  }

  public StackGresExtensionMetadata(StackGresClusterInstalledExtension installedExtension) {
    super();
    this.extension = new StackGresExtension();
    this.extension.setName(installedExtension.getName());
    this.extension.setRepository(installedExtension.getRepository());
    this.version = new StackGresExtensionVersion();
    this.version.setVersion(installedExtension.getVersion());
    this.version.setExtraMounts(installedExtension.getExtraMounts());
    this.target = new StackGresExtensionVersionTarget();
    this.target.setPostgresVersion(installedExtension.getPostgresVersion());
    this.target.setBuild(installedExtension.getBuild());
    this.target.setOs(ExtensionUtil.DEFAULT_OS);
    this.target.setArch(ExtensionUtil.DEFAULT_ARCH);
    this.publisher = new StackGresExtensionPublisher();
    this.build = new StackGresExtensionMetadataBuild(installedExtension.getBuild());
  }

  public StackGresExtension getExtension() {
    return extension;
  }

  public StackGresExtensionVersion getVersion() {
    return version;
  }

  @JsonIgnore
  public StackGresExtensionMetadataBuild getBuild() {
    return build;
  }

  public int getMajorBuild() {
    return build.getMajorBuild();
  }

  public int getMinorBuild() {
    return build.getMinorBuild();
  }

  public StackGresExtensionVersionTarget getTarget() {
    return target;
  }

  public StackGresExtensionPublisher getPublisher() {
    return publisher;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extension, publisher, target, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresExtensionMetadata)) {
      return false;
    }
    StackGresExtensionMetadata other = (StackGresExtensionMetadata) obj;
    return Objects.equals(extension, other.extension)
        && Objects.equals(publisher, other.publisher) && Objects.equals(target, other.target)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public int compareTo(StackGresExtensionMetadata o) {
    int compare = build.compareTo(o.build);
    if (compare == 0) {
      String[] versionParts = version.getVersion().split("\\.");
      String[] otherVersionParts = o.version.getVersion().split("\\.");
      for (int i = 0; i < versionParts.length && i < otherVersionParts.length; i++) {
        int versionPartNumber = getNumberFromVersionPart(versionParts[i]);
        int otherVersionPartNumber = getNumberFromVersionPart(otherVersionParts[i]);
        if (versionPartNumber != otherVersionPartNumber) {
          return versionPartNumber - otherVersionPartNumber;
        }
      }
    }
    return compare;
  }

  private int getNumberFromVersionPart(String versionPart) {
    try {
      return Integer.parseInt(versionPart);
    } catch (NumberFormatException ex) {
      return 0;
    }
  }
}
