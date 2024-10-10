/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;
import java.util.Optional;

import io.stackgres.common.OsDetector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatusAddon;

public class DocirFlavorIndex {
  private final String repository;
  private final String flavor;
  private final Integer flavorMajor;
  private final Integer flavorMinor;
  private final String base;
  private final Integer baseMajor;
  private final Integer baseMinor;
  private final String patroniVersion;
  private final String walgVersion;
  private final String hdrhistogramVersion;
  private final boolean fromIndex;
  private final String flavorRevision;
  private final String baseRevision;
  private final String patroniRevision;
  private final String walgRevision;
  private final String hdrhistogramRevision;
  private final Optional<String> arch;
  private final Optional<String> os;

  public static DocirFlavorIndex fromCluster(
      StackGresCluster cluster,
      StackGresClusterStatus clusterStatus,
      boolean detectOs) {
    return new DocirFlavorIndex(cluster, clusterStatus,
        Optional.of(OsDetector.OS_DETECTOR).filter(od -> detectOs));
  }

  private DocirFlavorIndex(
      StackGresCluster cluster,
      StackGresClusterStatus clusterStatus,
      Optional<OsDetector> osDetector) {
    this.repository = clusterStatus.getRepository();
    this.flavor = DocirUtil.getFlavorName(cluster);
    var postgresMajorMinor = DocirUtil.getPostgresMajorMinor(cluster.getSpec().getPostgres().getVersion());
    this.flavorMajor = postgresMajorMinor.major();
    this.flavorMinor = postgresMajorMinor.minor();
    this.base = clusterStatus.getBase();
    var baseMajorMinor = DocirUtil.getBaseMajorMinor(clusterStatus.getBaseVersion());
    this.baseMajor = baseMajorMinor.major();
    this.baseMinor = baseMajorMinor.minor();
    this.patroniVersion = clusterStatus.findAddon(DocirUtil.PATRONI_ADDON)
        .map(StackGresClusterStatusAddon::getVersion).orElse(null);
    this.walgVersion = clusterStatus.findAddon(DocirUtil.WALG_ADDON)
        .map(StackGresClusterStatusAddon::getVersion).orElse(null);
    this.hdrhistogramVersion = clusterStatus.findAddon(DocirUtil.HDRHISTOGRAM_ADDON)
        .map(StackGresClusterStatusAddon::getVersion).orElse(null);
    this.fromIndex = false;
    this.flavorRevision = clusterStatus.getRevision();
    this.baseRevision = clusterStatus.getBaseRevision();
    this.patroniRevision = clusterStatus.findAddon(DocirUtil.PATRONI_ADDON)
        .map(StackGresClusterStatusAddon::getRevision).orElse(null);
    this.walgRevision = clusterStatus.findAddon(DocirUtil.WALG_ADDON)
        .map(StackGresClusterStatusAddon::getRevision).orElse(null);
    this.hdrhistogramRevision = clusterStatus.findAddon(DocirUtil.HDRHISTOGRAM_ADDON)
        .map(StackGresClusterStatusAddon::getRevision).orElse(null);
    this.arch = OsDetector.getClusterArch(cluster, osDetector);
    this.os = OsDetector.getClusterOs(cluster, osDetector);
  }

  public DocirFlavorIndex(
      DocirFlavor flavor,
      DocirBase base,
      DocirAddonVersion patroni,
      DocirAddonVersion walg,
      DocirAddonVersion hdrhistogram) {
    this.repository = flavor.getRepository();
    this.flavor = flavor.getName();
    this.flavorMajor = flavor.getMajor();
    this.flavorMinor = flavor.getMinor();
    this.base = base.getName();
    this.baseMajor = base.getMajor();
    this.baseMinor = base.getMinor();
    this.patroniVersion = patroni.getVersion();
    this.walgVersion = walg.getVersion();
    this.hdrhistogramVersion = hdrhistogram.getVersion();
    this.fromIndex = true;
    this.flavorRevision = flavor.getRevision();
    this.baseRevision = base.getRevision();
    this.patroniRevision = patroni.getRevision();
    this.walgRevision = walg.getRevision();
    this.hdrhistogramRevision = hdrhistogram.getRevision();
    this.arch = Optional.of(flavor.getArchOrDefault());
    this.os = Optional.of(flavor.getOsOrDefault());
  }

  @Override
  public int hashCode() {
    return Objects.hash(flavor, flavorMinor, flavorMajor, repository);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirFlavorIndex)) {
      return false;
    }
    DocirFlavorIndex other = (DocirFlavorIndex) obj;
    if (Objects.equals(repository, other.repository)
        && Objects.equals(flavor, other.flavor)
        && Objects.equals(base, other.base)) {
      if ((fromIndex && other.fromIndex)
          || (!fromIndex && !other.fromIndex)) {
        return equalsBothFromIndex(this, other);
      }
      if (fromIndex && !other.fromIndex) {
        return equalsWithFromIndex(other, this);
      }
      if (!fromIndex && other.fromIndex) {
        return equalsWithFromIndex(this, other);
      }
    }
    return false;
  }

  private boolean equalsBothFromIndex(
      DocirFlavorIndex self,
      DocirFlavorIndex other) {
    return Objects.equals(self.flavorMajor, other.flavorMajor)
        && Objects.equals(self.flavorMinor, other.flavorMinor)
        && Objects.equals(self.baseMajor, other.baseMajor)
        && Objects.equals(self.baseMinor, other.baseMinor)
        && Objects.equals(self.patroniVersion, other.patroniVersion)
        && Objects.equals(self.walgVersion, other.walgVersion)
        && Objects.equals(self.hdrhistogramVersion, other.hdrhistogramVersion)
        && Objects.equals(self.arch, other.arch)
        && Objects.equals(self.os, other.os)
        && Objects.equals(self.flavorRevision, other.flavorRevision)
        && Objects.equals(self.baseRevision, other.baseRevision)
        && Objects.equals(self.patroniRevision, other.patroniRevision)
        && Objects.equals(self.walgRevision, other.walgRevision)
        && Objects.equals(self.hdrhistogramRevision, other.hdrhistogramRevision);
  }

  private boolean equalsWithFromIndex(
      DocirFlavorIndex other,
      DocirFlavorIndex fromIndex) {
    return fromIndex.flavorMajor.equals(other.flavorMajor)
        && fromIndex.flavorMinor.equals(other.flavorMinor)
        && fromIndex.baseMajor.equals(other.baseMajor)
        && fromIndex.baseMinor.equals(other.baseMinor)
        && fromIndex.patroniVersion.equals(other.patroniVersion)
        && fromIndex.walgVersion.equals(other.walgVersion)
        && fromIndex.hdrhistogramVersion.equals(other.hdrhistogramVersion)
        && (other.arch.isEmpty() || Objects.equals(fromIndex.arch, other.arch))
        && (other.os.isEmpty() || Objects.equals(fromIndex.os, other.os))
        && Objects.equals(fromIndex.flavorRevision, other.flavorRevision)
        && Objects.equals(fromIndex.baseRevision, other.baseRevision)
        && Objects.equals(fromIndex.patroniRevision, other.patroniRevision)
        && Objects.equals(fromIndex.walgRevision, other.walgRevision)
        && Objects.equals(fromIndex.hdrhistogramRevision, other.hdrhistogramRevision);
  }

  @Override
  public String toString() {
    return String.format(
        "%s/%s-%s.%s-%s-%s-%s",
        repository,
        flavor, flavorMajor, flavorMinor,
        flavorRevision != null ? "-revision-" + flavorRevision : "",
        os.orElse("any"), arch.orElse("any"));
  }

}
