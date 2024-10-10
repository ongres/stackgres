/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.component.Component.ComponentVersion;
import io.stackgres.common.component.Component.ComposedComponentVersion;
import io.stackgres.common.component.Components.ComponentWrapper;
import io.stackgres.common.docir.DocirAddonVersion;
import io.stackgres.common.docir.DocirFlavorMetadata;
import io.stackgres.common.docir.DocirRevision;
import io.stackgres.common.docir.DocirUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocirVersionReader implements VersionReader {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DocirVersionReader.class);

  private final ComponentWrapper componentWrapper;
  private final URI repositoryUri;

  /**
   * Create a reader of the versions served by the docir repository at {@code repositoryUri} (or
   * by the default repository when {@code null}) falling back to {@code componentWrapper} when
   * called without a {@link StackGresContext}.
   */
  DocirVersionReader(ComponentWrapper componentWrapper, URI repositoryUri) {
    this.componentWrapper = componentWrapper;
    this.repositoryUri = repositoryUri;
  }

  public URI getRepositoryUri() {
    return repositoryUri;
  }

  @Override
  public List<List<Component>> getSubComponents(StackGresContext context, Component component) {
    if (context == null) {
      if (componentWrapper == null) {
        throw new RuntimeException("No fallback set for component " + component
            + " and getComposedVersions called without context");
      }
      if (!componentWrapper.getComponent().getName().equals(component.getName())) {
        throw new RuntimeException("Fallback set for component " + component
            + " has a different name " + componentWrapper.getComponent());
      }
      LOGGER.debug("getSubComponents for component " + component.getComponent() + " was called without context");
      return componentWrapper.getComponent().getSubComponents();
    }
    return component.getSubComponents();
  }

  @Override
  public List<ComposedComponentVersion> getComposedVersions(StackGresContext context, Component component) {
    if (context == null) {
      if (componentWrapper == null) {
        throw new RuntimeException("No fallback set for component " + component
            + " and getComposedVersions called without context");
      }
      if (!componentWrapper.getComponent().getName().equals(component.getName())) {
        throw new RuntimeException("Fallback set for component " + component
            + " has a different name " + componentWrapper.getComponent());
      }
      LOGGER.debug("getComposedVersions for component " + component.getComponent() + " was called without context");
      return componentWrapper.getComponent().getVersionReader()
          .getComposedVersions(null, componentWrapper.getComponent());
    }
    if (DocirUtil.SIDECAR_ADDONS.containsKey(component.getComponent())) {
      final String addonName = DocirUtil.getAddonName(component.getComponent());
      return Seq.seq(context.getMetadataManager().getAddons(repositoryUri))
          .filter(addon -> Objects.equals(addon.getName(), addonName))
          .flatMap(addon -> addon.getVersions().stream())
          .map(version -> new DocirComposedComponentVersion(
              component, new DocirAddonComponentVersion(version), List.of()))
          .sorted(Comparator.reverseOrder())
          .map(ComposedComponentVersion.class::cast)
          .toList();
    }
    return Seq.seq(context.getMetadataManager().getFlavors(repositoryUri))
        .map(flavor -> createComposedComponentVersion(component, flavor))
        .flatMap(Optional::stream)
        .sorted(Comparator.reverseOrder())
        .map(ComposedComponentVersion.class::cast)
        .toList();
  }

  @Override
  public boolean hasImage() {
    return false;
  }

  Optional<ComposedComponentVersion> createComposedComponentVersion(
      Component component,
      DocirFlavorMetadata flavorMetadata) {
    switch (component.getComponent()) {
      case PATRONI:
        return Optional.of(new DocirComposedComponentVersion(
            component,
            new DocirPatroniVersion(flavorMetadata),
            List.of(
                Tuple.tuple(0, new DocirWalgVersion(flavorMetadata)),
                Tuple.tuple(0, new DocirHdrhistogramVersion(flavorMetadata)),
                Tuple.tuple(getFlavorIndex(flavorMetadata), new DocirFlavorVersion(flavorMetadata)))));
      case WALG:
        return Optional.of(new DocirComposedComponentVersion(
            component,
            new DocirWalgVersion(flavorMetadata),
            List.of()));
      case HDRHISTOGRAM:
        return Optional.of(new DocirComposedComponentVersion(
            component,
            new DocirHdrhistogramVersion(flavorMetadata),
            List.of()));
      case POSTGRESQL:
      case BABELFISH:
        if (!flavorMetadata.getFlavor().getName().equals(
                DocirUtil.getFlavorName(component))) {
          return Optional.empty();
        }
        return Optional.of(new DocirComposedComponentVersion(
            component,
            new DocirFlavorVersion(flavorMetadata),
            List.of()));
      default:
        throw new IllegalArgumentException("Component " + component + " can not be mapped to Docir images");
    }
  }

  private int getFlavorIndex(DocirFlavorMetadata flavorMetadata) {
    final StackGresComponent component = DocirUtil.getComponent(flavorMetadata);
    switch (component) {
      case POSTGRESQL:
        return 0;
      case BABELFISH:
        return 1;
      default:
        throw new IllegalArgumentException("Component " + component + " is not a flavor");
    }
  }

  public class DocirComposedComponentVersion implements ComposedComponentVersion {
    final Component component;
    final ComponentVersion version;
    final List<Tuple2<Integer, ComponentVersion>> subVersions;

    DocirComposedComponentVersion(
        Component component,
        ComponentVersion version,
        List<Tuple2<Integer, ComponentVersion>> subVersions) {
      this.component = component;
      this.version = version;
      this.subVersions = subVersions;
    }

    @Override
    public Component getComponent() {
      return component;
    }

    @Override
    public ComponentVersion getVersion() {
      return version;
    }

    @Override
    public List<Tuple2<Integer, ComponentVersion>> getSubVersions() {
      return subVersions;
    }

    @Override
    public String getImageName() {
      throw new UnsupportedOperationException("Can not call getImageName for a Docir image");
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hash(component, subVersions, version);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof DocirComposedComponentVersion)) {
        return false;
      }
      DocirComposedComponentVersion other = (DocirComposedComponentVersion) obj;
      return Objects.equals(component, other.component)
          && Objects.equals(subVersions, other.subVersions)
          && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
      return String.format("%s %s", version, subVersions);
    }
  }

  abstract class DocirFlavoredVersion implements ComponentVersion {

    final DocirFlavorMetadata flavor;
    final String version;
    final Integer major;
    final Integer minor;
    final Integer patch;
    final Integer versionType;
    final Integer suffixVersion;
    final String baseName;
    final Integer baseMajor;
    final Integer baseMinor;
    final Integer revision;

    public DocirFlavoredVersion(DocirFlavorMetadata flavor, String version, Integer major,
        Integer minor, Integer patch, Integer versionType, Integer suffixVersion,
        DocirRevision revision) {
      this.flavor = flavor;
      this.version = version;
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.versionType = versionType;
      this.suffixVersion = suffixVersion;
      this.baseName = flavor.getBase().getName();
      this.baseMajor = flavor.getBase().getMajor();
      this.baseMinor = flavor.getBase().getMinor();
      this.revision = Math.toIntExact(revision.getValue());
    }

    public DocirFlavorMetadata getFlavor() {
      return flavor;
    }

    @Override
    public String getVersion() {
      return version;
    }

    @Override
    public Integer getMajor() {
      return major;
    }

    @Override
    public Integer getMinor() {
      return minor;
    }

    @Override
    public Integer getPatch() {
      return patch;
    }

    @Override
    public Integer getVersionType() {
      return versionType;
    }

    @Override
    public Integer getSuffixVersion() {
      return suffixVersion;
    }

    @Override
    public String getBaseName() {
      return baseName;
    }

    @Override
    public Integer getBaseMajor() {
      return baseMajor;
    }

    @Override
    public Integer getBaseMinor() {
      return baseMinor;
    }

    @Override
    public Integer getRevision() {
      return revision;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hash(version, baseName, baseMajor, baseMinor, revision);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      DocirFlavoredVersion other = (DocirFlavoredVersion) obj;
      return Objects.equals(version, other.version)
          && Objects.equals(baseName, other.baseName)
          && Objects.equals(baseMajor, other.baseMajor)
          && Objects.equals(baseMinor, other.baseMinor)
          && Objects.equals(revision, other.revision);
    }

    @Override
    public String toString() {
      return String.format("%s-revision-%s", version, revision);
    }

  }

  public class DocirFlavorVersion extends DocirFlavoredVersion {

    DocirFlavorVersion(DocirFlavorMetadata flavor) {
      super(
          flavor,
          flavor.getFlavor().getVersion(),
          flavor.getFlavor().getMajor(),
          flavor.getFlavor().getMinor(),
          null,
          2,
          null,
          flavor.getFlavorRevision());
    }

    boolean matchPatroniVersion(DocirPatroniVersion patroniVersion) {
      return flavor.getPatroniVersion().equals(patroniVersion.getVersion())
          && flavor.getPatroniRevision().equals(patroniVersion.getFlavor().getPatroniRevision())
          && flavor.getFlavor().getName().equals(patroniVersion.getFlavor().getFlavor().getName())
          && flavor.getFlavor().getOsOrDefault().equals(patroniVersion.getFlavor().getFlavor().getOsOrDefault())
          && flavor.getFlavor().getArchOrDefault().equals(patroniVersion.getFlavor().getFlavor().getArchOrDefault());
    }

    boolean matchWalgVersion(DocirWalgVersion walgVersion) {
      return flavor.getWalgVersion().equals(walgVersion.getVersion())
          && flavor.getWalgRevision().equals(walgVersion.getFlavor().getWalgRevision())
          && flavor.getFlavor().getName().equals(walgVersion.getFlavor().getFlavor().getName())
          && flavor.getFlavor().getOsOrDefault().equals(walgVersion.getFlavor().getFlavor().getOsOrDefault())
          && flavor.getFlavor().getArchOrDefault().equals(walgVersion.getFlavor().getFlavor().getArchOrDefault());
    }

  }

  private static final Pattern VERSION_PATTERN = Pattern.compile(
      "^(?<version>(?<major>\\d+)"
          + "(?:\\.(?<minor>\\d+))?"
          + "(?:\\.(?<patch>\\d+))?"
          + "(?:-.*)?"
          + ")$");

  private static Matcher matcher(String addon, String version) {
    Matcher matcher = VERSION_PATTERN.matcher(version);
    if (!matcher.find()) {
      throw new IllegalArgumentException(addon + " version " + version
          + " does not match the pattern " + VERSION_PATTERN);
    }
    return matcher;
  }

  private static Integer groupOrNull(Matcher matcher, String group) {
    return Optional.ofNullable(matcher.group(group)).map(Integer::parseInt).orElse(null);
  }

  public class DocirPatroniVersion extends DocirFlavoredVersion {

    DocirPatroniVersion(DocirFlavorMetadata flavor) {
      this(flavor, matcher(flavor));
    }

    private static Matcher matcher(DocirFlavorMetadata flavor) {
      return DocirVersionReader.matcher(DocirUtil.PATRONI_ADDON, flavor.getPatroniVersion());
    }

    DocirPatroniVersion(DocirFlavorMetadata flavor, Matcher matcher) {
      super(
          flavor,
          flavor.getPatroniVersion(),
          Integer.parseInt(matcher.group("major")),
          groupOrNull(matcher, "minor"),
          groupOrNull(matcher, "patch"),
          2,
          null,
          flavor.getPatroniRevision());
    }

  }

  public class DocirWalgVersion extends DocirFlavoredVersion {

    DocirWalgVersion(DocirFlavorMetadata flavor) {
      this(flavor, matcher(flavor));
    }

    private static Matcher matcher(DocirFlavorMetadata flavor) {
      return DocirVersionReader.matcher(DocirUtil.WALG_ADDON, flavor.getWalgVersion());
    }

    DocirWalgVersion(DocirFlavorMetadata flavor, Matcher matcher) {
      super(
          flavor,
          flavor.getWalgVersion(),
          Integer.parseInt(matcher.group("major")),
          groupOrNull(matcher, "minor"),
          groupOrNull(matcher, "patch"),
          2,
          null,
          flavor.getWalgRevision());
    }

    boolean matchPatroniVersion(DocirPatroniVersion patroniVersion) {
      return flavor.getPatroniVersion().equals(patroniVersion.getVersion())
          && flavor.getPatroniRevision().equals(patroniVersion.getFlavor().getPatroniRevision())
          && flavor.getFlavor().getName().equals(patroniVersion.getFlavor().getFlavor().getName())
          && flavor.getFlavor().getOsOrDefault().equals(patroniVersion.getFlavor().getFlavor().getOsOrDefault())
          && flavor.getFlavor().getArchOrDefault().equals(patroniVersion.getFlavor().getFlavor().getArchOrDefault());
    }

  }

  public class DocirHdrhistogramVersion extends DocirFlavoredVersion {

    DocirHdrhistogramVersion(DocirFlavorMetadata flavor) {
      this(flavor, matcher(DocirUtil.HDRHISTOGRAM_ADDON, flavor.getHdrhistogramVersion()));
    }

    DocirHdrhistogramVersion(DocirFlavorMetadata flavor, Matcher matcher) {
      super(
          flavor,
          flavor.getHdrhistogramVersion(),
          Integer.parseInt(matcher.group("major")),
          groupOrNull(matcher, "minor"),
          groupOrNull(matcher, "patch"),
          2,
          null,
          flavor.getHdrhistogramRevision());
    }

  }

  /**
   * The version of an addon that is combined alone with the base image and the Postgres flavor in
   * the image of a sidecar (or Job) container.
   */
  public class DocirAddonComponentVersion implements ComponentVersion {

    final DocirAddonVersion addon;
    final Integer major;
    final Integer minor;
    final Integer patch;
    final Integer revision;

    DocirAddonComponentVersion(DocirAddonVersion addon) {
      this.addon = addon;
      final Matcher matcher = matcher(addon.getBaseIdentity(), addon.getVersion());
      this.major = Integer.parseInt(matcher.group("major"));
      this.minor = groupOrNull(matcher, "minor");
      this.patch = groupOrNull(matcher, "patch");
      this.revision = Math.toIntExact(new DocirRevision(addon.getRevision()).getValue());
    }

    public DocirAddonVersion getAddon() {
      return addon;
    }

    @Override
    public String getVersion() {
      return addon.getVersion();
    }

    @Override
    public Integer getMajor() {
      return major;
    }

    @Override
    public Integer getMinor() {
      return minor;
    }

    @Override
    public Integer getPatch() {
      return patch;
    }

    @Override
    public Integer getVersionType() {
      return 2;
    }

    @Override
    public Integer getSuffixVersion() {
      return null;
    }

    @Override
    public String getBaseName() {
      return addon.getBaseName();
    }

    @Override
    public Integer getBaseMajor() {
      return addon.getBaseMajor();
    }

    @Override
    public Integer getBaseMinor() {
      return addon.getBaseMinor();
    }

    @Override
    public Integer getRevision() {
      return revision;
    }

    @Override
    public int hashCode() {
      return Objects.hash(addon);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof DocirAddonComponentVersion)) {
        return false;
      }
      DocirAddonComponentVersion other = (DocirAddonComponentVersion) obj;
      return Objects.equals(addon, other.addon);
    }

    @Override
    public String toString() {
      return String.format("%s-revision-%s", addon.getVersion(), revision);
    }

  }

}
