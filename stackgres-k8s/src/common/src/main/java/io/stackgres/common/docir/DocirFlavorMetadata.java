/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.docir;

import java.util.Objects;

import io.stackgres.common.StackGresUtil;

/**
 * A Postgres flavor version together with the base image it was built on and the versions of the
 * addons combined with it in the image of the patroni container (patroni, wal-g and hdrhistogram),
 * all built on the same base image.
 */
public class DocirFlavorMetadata
    implements Comparable<DocirFlavorMetadata> {

  private final DocirFlavor flavor;
  private final DocirRevision flavorRevision;
  private final DocirBase base;
  private final String baseRevision;
  private final String patroniVersion;
  private final DocirRevision patroniRevision;
  private final String walgVersion;
  private final DocirRevision walgRevision;
  private final String hdrhistogramVersion;
  private final DocirRevision hdrhistogramRevision;

  public DocirFlavorMetadata(
      DocirFlavor flavor,
      DocirBase base,
      DocirAddonVersion patroni,
      DocirAddonVersion walg,
      DocirAddonVersion hdrhistogram) {
    super();
    this.flavor = flavor;
    this.flavorRevision = new DocirRevision(flavor.getRevision());
    this.base = base;
    this.baseRevision = base.getRevision();
    this.patroniVersion = patroni.getVersion();
    this.patroniRevision = new DocirRevision(patroni.getRevision());
    this.walgVersion = walg.getVersion();
    this.walgRevision = new DocirRevision(walg.getRevision());
    this.hdrhistogramVersion = hdrhistogram.getVersion();
    this.hdrhistogramRevision = new DocirRevision(hdrhistogram.getRevision());
  }

  public DocirFlavorMetadata(
      String flavor,
      Integer flavorMajor,
      Integer flavorMinor,
      String flavorRevision,
      String base,
      Integer baseMajor,
      Integer baseMinor,
      String baseRevision,
      String patroniVersion,
      String patroniRevision,
      String walgVersion,
      String walgRevision,
      String hdrhistogramVersion,
      String hdrhistogramRevision,
      String repository) {
    super();
    this.flavor = new DocirFlavor();
    this.flavor.setName(flavor);
    this.flavor.setMajor(flavorMajor);
    this.flavor.setMinor(flavorMinor);
    this.flavor.setRepository(repository);
    this.flavor.setRevision(flavorRevision);
    this.flavorRevision = new DocirRevision(flavorRevision);
    this.base = new DocirBase();
    this.base.setName(base);
    this.base.setMajor(baseMajor);
    this.base.setMinor(baseMinor);
    this.base.setRepository(repository);
    this.base.setRevision(baseRevision);
    this.baseRevision = baseRevision;
    this.patroniVersion = patroniVersion;
    this.patroniRevision = new DocirRevision(patroniRevision);
    this.walgVersion = walgVersion;
    this.walgRevision = new DocirRevision(walgRevision);
    this.hdrhistogramVersion = hdrhistogramVersion;
    this.hdrhistogramRevision = new DocirRevision(hdrhistogramRevision);
  }

  public DocirBase getBase() {
    return base;
  }

  public String getBaseRevision() {
    return baseRevision;
  }

  public DocirFlavor getFlavor() {
    return flavor;
  }

  public DocirRevision getFlavorRevision() {
    return flavorRevision;
  }

  public String getPatroniVersion() {
    return patroniVersion;
  }

  public DocirRevision getPatroniRevision() {
    return patroniRevision;
  }

  public String getWalgVersion() {
    return walgVersion;
  }

  public DocirRevision getWalgRevision() {
    return walgRevision;
  }

  public String getHdrhistogramVersion() {
    return hdrhistogramVersion;
  }

  public DocirRevision getHdrhistogramRevision() {
    return hdrhistogramRevision;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        base.getName(), base.getMajor(), base.getMinor(), baseRevision,
        flavor.getName(), flavor.getMajor(), flavor.getMinor(), flavorRevision,
        patroniVersion, patroniRevision, walgVersion, walgRevision,
        hdrhistogramVersion, hdrhistogramRevision);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DocirFlavorMetadata)) {
      return false;
    }
    DocirFlavorMetadata other = (DocirFlavorMetadata) obj;
    return Objects.equals(flavor.getName(), other.flavor.getName())
        && Objects.equals(flavor.getMajor(), other.flavor.getMajor())
        && Objects.equals(flavor.getMinor(), other.flavor.getMinor())
        && Objects.equals(flavorRevision, other.flavorRevision)
        && Objects.equals(base.getName(), other.base.getName())
        && Objects.equals(base.getMajor(), other.base.getMajor())
        && Objects.equals(base.getMinor(), other.base.getMinor())
        && Objects.equals(baseRevision, other.baseRevision)
        && Objects.equals(patroniVersion, other.patroniVersion)
        && Objects.equals(patroniRevision, other.patroniRevision)
        && Objects.equals(walgVersion, other.walgVersion)
        && Objects.equals(walgRevision, other.walgRevision)
        && Objects.equals(hdrhistogramVersion, other.hdrhistogramVersion)
        && Objects.equals(hdrhistogramRevision, other.hdrhistogramRevision);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public int compareTo(DocirFlavorMetadata o) {
    int compare = flavor.getMajor() - o.flavor.getMajor();
    if (compare == 0) {
      compare = flavor.getMinor() - o.flavor.getMinor();
    }
    if (compare == 0) {
      compare = base.getMajor() - o.base.getMajor();
    }
    if (compare == 0) {
      compare = base.getMinor() - o.base.getMinor();
    }
    if (compare == 0) {
      compare = compareVersions(patroniVersion, o.patroniVersion);
    }
    if (compare == 0) {
      compare = compareVersions(walgVersion, o.walgVersion);
    }
    if (compare == 0) {
      compare = compareVersions(hdrhistogramVersion, o.hdrhistogramVersion);
    }
    if (compare == 0) {
      compare = flavorRevision.compareTo(o.flavorRevision);
    }
    if (compare == 0) {
      compare = new DocirRevision(baseRevision).compareTo(new DocirRevision(o.baseRevision));
    }
    if (compare == 0) {
      compare = patroniRevision.compareTo(o.patroniRevision);
    }
    if (compare == 0) {
      compare = walgRevision.compareTo(o.walgRevision);
    }
    if (compare == 0) {
      compare = hdrhistogramRevision.compareTo(o.hdrhistogramRevision);
    }
    return compare;
  }

  private static int compareVersions(String version, String other) {
    if (version == null || other == null) {
      return Objects.equals(version, other) ? 0 : (version == null ? -1 : 1);
    }
    return StackGresUtil.sortableVersion(version).compareTo(StackGresUtil.sortableVersion(other));
  }

}
