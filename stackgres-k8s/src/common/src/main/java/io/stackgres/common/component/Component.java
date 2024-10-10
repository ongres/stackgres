/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import io.stackgres.common.StackGresComponent;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class Component {

  final VersionReader versionReader;
  final StackGresComponent component;
  final List<List<Component>> subComponents;

  Component(StackGresComponent component, VersionReader versionReader, Component[]... subComponents) {
    this.component = component;
    this.versionReader = versionReader;
    this.subComponents = Seq.of(subComponents)
        .map(subComponentArray -> List.copyOf(Arrays.asList(subComponentArray)))
        .toList();
  }

  public VersionReader getVersionReader() {
    return versionReader;
  }

  public StackGresComponent getComponent() {
    return component;
  }

  public String getName() {
    return component.getName();
  }

  public List<List<Component>> getSubComponents() {
    return subComponents;
  }

  public boolean hasImage() {
    return versionReader.hasImage();
  }

  public List<ComposedComponentVersion> getComposedVersions(StackGresContext context) {
    return versionReader.getComposedVersions(context, this);
  }

  public interface ComposedComponentVersion extends Comparable<ComposedComponentVersion> {

    Component getComponent();

    ComponentVersion getVersion();

    List<Tuple2<Integer, ComponentVersion>> getSubVersions();

    default List<ComponentVersion> getSubComponentVersions() {
      return getSubVersions()
          .stream()
          .sorted(Comparator.comparing(Tuple2::v1))
          .map(Tuple2::v2)
          .toList();
    }

    String getImageName();

    @Override
    default int compareTo(ComposedComponentVersion o) {
      int compare = getVersion().compareTo(o.getVersion());
      int index = 0;
      while (compare == 0 && index < getSubVersions().size()) {
        compare = getSubVersions().get(index).compareTo(o.getSubVersions().get(index));
        index++;
      }
      return compare;
    }

  }

  public interface ComponentVersion extends Comparable<ComponentVersion> {

    String getVersion();

    Integer getMajor();

    Integer getMinor();

    Integer getPatch();

    Integer getVersionType();

    Integer getSuffixVersion();

    String getBaseName();

    Integer getBaseMajor();

    Integer getBaseMinor();

    Integer getRevision();

    default String getBuild() {
      if (getBaseName() == null) {
        return Optional.ofNullable(getBaseMajor())
            .map(baseMajor -> baseMajor
                + Optional.ofNullable(getBaseMinor()).map(baseMinor -> "." + baseMinor).orElse(""))
            .orElse(null);
      }
      return getBuildMajorVersion() + "-" + getRevision();
    }

    default String getBuildMajorVersion() {
      if (getBaseName() == null) {
        return Optional.ofNullable(getBaseMajor()).map(Object::toString).orElse(null);
      }
      return getBaseName() + "-" + getBaseMajor() + "." + getBaseMinor();
    }

    @Override
    default int compareTo(ComponentVersion o) {
      int compare = getMajor().compareTo(o.getMajor());
      if (compare == 0 && getMinor() != null && o.getMinor() != null) {
        compare = getMinor().compareTo(o.getMinor());
      }
      if (compare == 0 && getPatch() != null && o.getPatch() != null) {
        compare = getPatch().compareTo(o.getPatch());
      }
      if (compare == 0 && getVersionType() != null && o.getVersionType() != null) {
        compare = getVersionType().compareTo(o.getVersionType());
      }
      if (compare == 0 && getSuffixVersion() != null && o.getSuffixVersion() != null) {
        compare = getSuffixVersion().compareTo(o.getSuffixVersion());
      }
      if (compare == 0 && getBaseMajor() != null && o.getBaseMajor() != null) {
        compare = getBaseMajor().compareTo(o.getBaseMajor());
      }
      if (compare == 0 && getBaseMinor() != null && o.getBaseMinor() != null) {
        compare = getBaseMinor().compareTo(o.getBaseMinor());
      }
      if (compare == 0 && getRevision() != null && o.getRevision() != null) {
        compare = getRevision().compareTo(o.getRevision());
      }
      return compare;
    }

    default int compareToBuild(String build) {
      int indexOfSeparator = build.indexOf('.');
      if (indexOfSeparator < 1) {
        throw new IllegalArgumentException(build + " is not a build version");
      }
      int buildMajor = Integer.parseInt(build.substring(0, indexOfSeparator));
      int buildMinor = Integer.parseInt(build.substring(indexOfSeparator + 1));
      int compare = getMajor().compareTo(buildMajor);
      if (compare == 0 && getMinor() != null) {
        compare = getMinor().compareTo(buildMinor);
      }
      return compare;
    }

  }

  public Optional<String> findLatestImageName(StackGresContext context) {
    return findImageName(context, StackGresComponent.LATEST, Seq.seq(
        versionReader.getSubComponents(context, this))
        .map(alternativeSubComponents -> alternativeSubComponents.getFirst())
        .collect(ImmutableMap.toImmutableMap(
            Function.identity(), subComponent -> StackGresComponent.LATEST)));
  }

  public String getLatestImageName(StackGresContext context) {
    return getImageName(context, StackGresComponent.LATEST, Seq.seq(
        versionReader.getSubComponents(context, this))
        .map(alternativeSubComponents -> alternativeSubComponents.getFirst())
        .collect(ImmutableMap.toImmutableMap(
            Function.identity(), subComponent -> StackGresComponent.LATEST)));
  }

  public Optional<String> findImageName(StackGresContext context, String version) {
    return findImageName(context, version, Map.of());
  }

  public Optional<String> findImageName(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    checkSubComponents(context, subComponentVersions);
    return findComposedVersion(context, version, subComponentVersions)
        .map(ComposedComponentVersion::getImageName)
        .findFirst();
  }

  public String getImageName(StackGresContext context, String version) {
    return getImageName(context, version, Map.of());
  }

  public String getImageName(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    return findImageName(context, version, subComponentVersions)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " and sub-components "
                + subComponentVersions + " not available"));
  }

  public Optional<String> findLatestVersion(StackGresContext context) {
    return findVersion(context, StackGresComponent.LATEST);
  }

  public String getLatestVersion(StackGresContext context) {
    return getVersion(context, StackGresComponent.LATEST);
  }

  public String getLatestVersion(StackGresContext context, Map<Component, String> subComponents) {
    return getVersion(context, StackGresComponent.LATEST, subComponents);
  }

  public Optional<String> findVersion(StackGresContext context, String version) {
    return findLatestBuildVersion(context, version)
        .map(ComponentVersion::getVersion);
  }

  public Optional<String> findVersion(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    return findComposedVersion(context, version, subComponentVersions)
        .map(ComposedComponentVersion::getVersion)
        .map(ComponentVersion::getVersion)
        .findFirst();
  }

  public String getVersion(StackGresContext context, String version) {
    return findVersion(context, version)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " not available"));
  }

  public String getVersion(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    return findVersion(context, version, subComponentVersions)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " not available"
                + " for " + subComponentVersions));
  }

  public Optional<String> findLatestMajorVersion(StackGresContext context) {
    return findMajorVersion(context, StackGresComponent.LATEST);
  }

  public String getLatestMajorVersion(StackGresContext context) {
    return getMajorVersion(context, StackGresComponent.LATEST);
  }

  public Optional<String> findMajorVersion(StackGresContext context, String version) {
    return findLatestBuildVersion(context, version)
        .map(ComponentVersion::getMajor)
        .map(Object::toString);
  }

  public String getMajorVersion(StackGresContext context, String version) {
    return findMajorVersion(context, version)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " not available"));
  }

  public Optional<String> findBuildVersion(StackGresContext context, String version) {
    return findLatestBuildVersion(context, version)
        .map(ComponentVersion::getBuild)
        .map(Object::toString);
  }

  public Optional<String> findBuildVersion(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    checkSubComponents(context, subComponentVersions);
    return findComposedVersion(context, version, subComponentVersions)
        .map(ComposedComponentVersion::getVersion)
        .map(ComponentVersion::getBuild)
        .findFirst();
  }

  public String getBuildVersion(StackGresContext context, String version) {
    return findBuildVersion(context, version)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " not available"));
  }

  public String getBuildVersion(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    return findBuildVersion(context, version, subComponentVersions)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " and sub-components "
                + subComponentVersions + " not available"));
  }

  public Optional<String> findBuildMajorVersion(StackGresContext context, String version) {
    return findLatestBuildVersion(context, version)
        .map(ComponentVersion::getBuildMajorVersion);
  }

  public String getBuildMajorVersion(StackGresContext context, String version) {
    return findBuildMajorVersion(context, version)
        .orElseThrow(() -> new IllegalArgumentException(
            component + " version " + version + " not available"));
  }

  private Optional<ComponentVersion> findLatestBuildVersion(StackGresContext context, String version) {
    return streamOrderedTagVersions(context)
        .filter(v -> isVersion(version, v))
        .findFirst();
  }

  private boolean isVersion(String version, ComponentVersion v) {
    return version == null
        || StackGresComponent.LATEST.equals(version)
        || v.getVersion().equals(version)
        || v.getVersion().startsWith(version + ".");
  }

  public Seq<String> streamOrderedVersions(StackGresContext context) {
    return streamOrderedComposedVersions(context)
        .map(ComposedComponentVersion::getVersion)
        .map(ComponentVersion::getVersion)
        .zipWithIndex()
        .grouped(Tuple2::v1)
        .map(t -> t.v2.get(0).get())
        .sorted(Comparator.comparing(t -> t.v2))
        .map(t -> t.v1);
  }

  public Seq<String> streamOrderedVersions(StackGresContext context, String build) {
    return streamOrderedComposedVersions(context)
        .map(ComposedComponentVersion::getVersion)
        .filter(imageVersion -> imageVersion.getBuild().equals(build))
        .map(ComponentVersion::getVersion)
        .zipWithIndex()
        .grouped(Tuple2::v1)
        .map(t -> t.v2.get(0).get())
        .sorted(Comparator.comparing(t -> t.v2))
        .map(t -> t.v1);
  }

  public Seq<String> streamOrderedMajorVersions(StackGresContext context) {
    return streamOrderedComposedVersions(context)
        .map(ComposedComponentVersion::getVersion)
        .map(ComponentVersion::getMajor)
        .map(Object::toString)
        .zipWithIndex()
        .grouped(Tuple2::v1)
        .map(t -> t.v2.get(0).get())
        .sorted(Comparator.comparing(t -> t.v2))
        .map(t -> t.v1);
  }

  public Seq<String> streamOrderedMajorVersions(StackGresContext context, String build) {
    return streamOrderedComposedVersions(context)
        .map(ComposedComponentVersion::getVersion)
        .filter(imageVersion -> imageVersion.getBuild().equals(build))
        .map(ComponentVersion::getMajor)
        .map(Object::toString)
        .zipWithIndex()
        .grouped(Tuple2::v1)
        .map(t -> t.v2.get(0).get())
        .sorted(Comparator.comparing(t -> t.v2))
        .map(t -> t.v1);
  }

  public Seq<String> streamOrderedBuildVersions(StackGresContext context) {
    return streamOrderedTagVersions(context)
        .map(ComponentVersion::getBuild)
        .filter(Objects::nonNull)
        .zipWithIndex()
        .grouped(Tuple2::v1)
        .map(t -> t.v2.get(0).get())
        .sorted(Comparator.comparing(t -> t.v2))
        .map(t -> t.v1);
  }

  public Seq<String> streamOrderedImageNames(StackGresContext context) {
    return streamOrderedComposedVersions(context)
        .map(ComposedComponentVersion::getImageName);
  }

  public Seq<ComponentVersion> streamOrderedTagVersions(StackGresContext context) {
    return streamOrderedComposedVersions(context)
        .map(ComposedComponentVersion::getVersion)
        .grouped(Function.identity())
        .sorted(Comparator.comparing(
            (Function<Tuple2<ComponentVersion, Seq<ComponentVersion>>, ComponentVersion>) Tuple2::v1)
            .reversed())
        .map(t -> t.v1);
  }

  public Seq<ComposedComponentVersion> streamOrderedComposedVersions(StackGresContext context) {
    return Seq.seq(getComposedVersions(context));
  }

  @Override
  public String toString() {
    return component.name();
  }

  public Seq<ComposedComponentVersion> findComposedVersion(
      StackGresContext context,
      String version,
      Map<Component, String> subComponentVersions) {
    checkSubComponents(context, subComponentVersions);
    return streamOrderedComposedVersions(context)
        .filter(cv -> isVersion(version, cv.getVersion()))
        .filter(cv -> Seq.seq(cv.getSubVersions())
            .zipWithIndex()
            .map(subVersion -> Tuple.tuple(
                versionReader
                .getSubComponents(context, this)
                .get(subVersion.v2.intValue())
                .get(subVersion.v1.v1),
                subVersion.v1.v2))
            .allMatch(subComponentVersion -> subComponentVersions.containsKey(subComponentVersion.v1)
                && isVersion(subComponentVersions.get(subComponentVersion.v1), subComponentVersion.v2)));
  }

  private void checkSubComponents(
      StackGresContext context,
      Map<Component, String> subComponentVersions) {
    List<List<Component>> subComponents = versionReader.getSubComponents(context, this);
    Preconditions.checkArgument(Seq.seq(subComponents)
        .allMatch(alternativeSubComponents -> alternativeSubComponents.stream()
            .anyMatch(subComponentVersions::containsKey)),
        "You must specify sub component versions for "
            + Seq.seq(subComponents)
            .filter(alternativeSubComponents -> alternativeSubComponents.stream()
                .noneMatch(subComponentVersions::containsKey))
            .map(alternativeSubComponent -> Seq.seq(alternativeSubComponent).toString(" or "))
            .toString(", "));
  }

  public static int compareBuildVersions(String leftBuildVersion, String rightBuildVersion) {
    return buildVersionAsNumber(leftBuildVersion) - buildVersionAsNumber(rightBuildVersion);
  }

  public static int buildVersionAsNumber(String buildVersion) {
    String[] buildVersionChunks = buildVersion.split("\\.");
    return Integer.parseInt(buildVersionChunks[0]) * 1000
        + Integer.parseInt(
            buildVersionChunks[1].endsWith("-dev")
            ? buildVersionChunks[1].substring(0, buildVersionChunks[1].length() - "-dev".length())
                : buildVersionChunks[1]);
  }

  @Override
  public int hashCode() {
    return Objects.hash(component, subComponents);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Component)) {
      return false;
    }
    Component other = (Component) obj;
    return Objects.equals(component, other.component)
        && Objects.equals(subComponents, other.subComponents);
  }

}
