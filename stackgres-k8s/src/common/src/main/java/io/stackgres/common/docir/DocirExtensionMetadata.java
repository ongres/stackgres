/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public class DocirExtensionMetadata
    implements Comparable<DocirExtensionMetadata> {

  private final DocirExtension extension;
  private final DocirExtensionVersion version;
  private final DocirRevision revision;
  private final String sortableVersion;

  public DocirExtensionMetadata(
      DocirExtension extension,
      DocirExtensionVersion version) {
    this.extension = extension;
    this.version = version;
    this.revision = new DocirRevision(version.getRevision());
    this.sortableVersion = StackGresUtil.sortableVersion(version.getVersion());
  }

  public DocirExtensionMetadata(StackGresClusterInstalledExtension installedExtension) {
    this.extension = new DocirExtension();
    this.extension.setName(installedExtension.getName());
    this.extension.setRepository(installedExtension.getRepository());
    this.version = new DocirExtensionVersion();
    this.version.setVersion(installedExtension.getVersion());
    this.version.setFlavorVersion(installedExtension.getPostgresVersion());
    this.version.setRevision(installedExtension.getBuild());
    this.revision = new DocirRevision(installedExtension.getBuild());
    this.sortableVersion = StackGresUtil.sortableVersion(installedExtension.getVersion());
  }

  public DocirExtension getExtension() {
    return extension;
  }

  public DocirExtensionVersion getVersion() {
    return version;
  }

  @JsonIgnore
  public DocirRevision getRevision() {
    return revision;
  }

  public String getSortableVersion() {
    return sortableVersion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(extension, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirExtensionMetadata)) {
      return false;
    }
    DocirExtensionMetadata other = (DocirExtensionMetadata) obj;
    return Objects.equals(extension, other.extension)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public int compareTo(DocirExtensionMetadata o) {
    int compare = sortableVersion.compareTo(o.sortableVersion);
    if (compare == 0) {
      compare = revision.compareTo(o.revision);
    }
    return compare;
  }

}
