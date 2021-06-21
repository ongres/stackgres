/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;

public enum StackGresComponent {

  POSTGRESQL("postgresql", StackGresProperty.SG_IMAGE_POSTGRESQL,
      "%1$s/ongres/postgresql:v%2$s-build-%3$s"),
  PATRONI("patroni", StackGresProperty.SG_IMAGE_PATRONI,
      "%1$s/ongres/patroni:v%2$s-pg%4$s-build-%3$s", StackGresComponent.POSTGRESQL),
  POSTGRES_UTIL("postgresql", StackGresProperty.SG_IMAGE_POSTGRES_UTIL,
      "%1$s/ongres/postgres-util:v%2$s-build-%3$s"),
  PGBOUNCER("pgbouncer", StackGresProperty.SG_IMAGE_PGBOUNCER,
      "%1$s/ongres/pgbouncer:v%2$s-build-%3$s"),
  PROMETHEUS_POSTGRES_EXPORTER("prometheus-postgres-exporter",
      StackGresProperty.SG_IMAGE_PROMETHEUS_POSTGRES_EXPORTER,
      "%1$s/ongres/prometheus-postgres-exporter:v%2$s-build-%3$s"),
  ENVOY("envoy", StackGresProperty.SG_IMAGE_ENVOY,
      "%1$s/ongres/envoy:v%2$s-build-%3$s"),
  FLUENT_BIT("fluentbit", StackGresProperty.SG_IMAGE_FLUENT_BIT,
      "%1$s/ongres/fluentbit:v%2$s-build-%3$s"),
  FLUENTD("fluentd", StackGresProperty.SG_IMAGE_FLUENTD,
      "%1$s/ongres/fluentd:v%2$s-build-%3$s"),
  KUBECTL("kubectl", StackGresProperty.SG_IMAGE_KUBECTL,
      "%1$s/ongres/kubectl:v%2$s-build-%3$s");

  public static final String LATEST = "latest";

  private static final String ARRAY_SPLIT_REGEXP = ",";

  final String name;
  final StackGresProperty imageTemplateProperty;
  final String defaultImageTemplate;
  final StackGresProperty componentVersionProperty;
  final List<StackGresComponent> subComponents;

  StackGresComponent(String name, StackGresProperty imageTemplateProperty,
      String defaultImageTemplate, StackGresComponent...subComponents) {
    this(name, imageTemplateProperty, null, defaultImageTemplate, subComponents);
  }

  StackGresComponent(String name, StackGresProperty imageTemplateProperty,
      StackGresProperty componentVersionProperty,
      String defaultImageTemplate, StackGresComponent...subComponents) {
    this.name = name;
    this.imageTemplateProperty = imageTemplateProperty;
    this.defaultImageTemplate = defaultImageTemplate;
    this.componentVersionProperty = componentVersionProperty;
    this.subComponents = ImmutableList.copyOf(subComponents);
  }

  private String imageTemplate() {
    return imageTemplateProperty.get()
        .map(template -> template.replace("${containerRegistry}", "%1$s"))
        .map(template -> template.replace(
            "${" + name.replaceAll("[^a-z]", "") + "Version}", "%2$s"))
        .map(template -> template.replace("${buildVersion}", "%3$s"))
        .map(template -> Seq.seq(subComponents)
            .zipWithIndex()
            .reduce(template, (templateResult, t) -> templateResult
                .replace("${" + t.v1.name.replaceAll("[^a-z]", "") + "Version}",
                    "%" + (t.v2 + 4) + "$s"),
                (u, v) -> v))
        .orElse(defaultImageTemplate);
  }

  private ImmutableList<ImageVersion> versions() {
    return Optional.ofNullable(componentVersionProperty)
        .flatMap(StackGresProperty::get)
        .map(ImageVersion::new)
        .map(ImmutableList::of)
        .orElseGet(() -> Seq.of(VersionReader.INSTANCE.getAsArray(this))
            .map(ImageVersion::new)
            .collect(ImmutableList.toImmutableList()));
  }

  private List<ComposedVersion> composedVersions() {
    return Seq.seq(this.subComponents)
        .map(subComponentVersions()::get)
        .<List<ComposedVersion>>reduce(
            Seq.seq(versions()).map(ComposedVersion::new).toList(),
            (composedVersions, subVersions) -> Seq.seq(composedVersions)
                .map(ComposedVersion::getVersions)
                .innerJoin(Seq.seq(subVersions),
                    (versions, subVersion) -> subVersion.build.equals(versions.get(0).build))
                .map(t -> Seq.seq(t.v1).append(t.v2).toList())
                .map(ComposedVersion::new)
                .toList(),
            (u, v) -> v);
  }

  private ImmutableMap<StackGresComponent, List<ImageVersion>> subComponentVersions() {
    return Seq.range(0, subComponents.size())
                .map(subComponent -> Tuple.tuple(subComponent,
                    Seq.of(VersionReader.INSTANCE.getAsArray(this, subComponent))
                    .map(ImageVersion::new)
                    .collect(ImmutableList.toImmutableList())))
                .collect(ImmutableMap.toImmutableMap(
                    t -> subComponents.get(t.v1),
                    t -> t.v2));
  }

  class ComposedVersion implements Comparable<ComposedVersion> {
    final List<ImageVersion> versions;

    public ComposedVersion(ImageVersion version) {
      this.versions = ImmutableList.of(version);
    }

    public ComposedVersion(List<ImageVersion> versions) {
      this.versions = versions;
    }

    public List<ImageVersion> getVersions() {
      return versions;
    }

    @Override
    public int compareTo(ComposedVersion o) {
      int compare = 0;
      int index = 0;
      while (compare == 0 && index < versions.size()) {
        compare = versions.get(index).compareTo(o.versions.get(index));
        index++;
      }
      return compare;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + Objects.hash(versions);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ComposedVersion)) {
        return false;
      }
      ComposedVersion other = (ComposedVersion) obj;
      if (!getEnclosingInstance().equals(other.getEnclosingInstance())) {
        return false;
      }
      return Objects.equals(versions, other.versions);
    }

    public ImageVersion getVersion() {
      if (this.versions.isEmpty()) {
        throw new IllegalArgumentException(
            StackGresComponent.this.name + " versions not configured");
      }
      return this.versions.get(0);
    }

    public ImageVersion getVersion(StackGresComponent subComponent) {
      final int subComponentIndex = StackGresComponent.this.subComponents.indexOf(subComponent);
      if (subComponentIndex < 0) {
        throw new IllegalArgumentException(
            "Sub-component " + subComponent.name + " not configured for component "
                + StackGresComponent.this.name);
      }
      if (subComponentIndex >= this.versions.size()) {
        throw new IllegalArgumentException(
            "Sub-component " + subComponent.name + " versions not configured for component "
                + StackGresComponent.this.name);
      }
      return this.versions.get(1 + subComponentIndex);
    }

    public String getImageName() {
      return String.format(StackGresComponent.this.imageTemplate(),
          Seq.of(StackGresProperty.SG_CONTAINER_REGISTRY.getString())
          .append(Seq.of(getVersion().getVersion(), getVersion().getBuild())
            .append(Seq.seq(versions).skip(1).map(ImageVersion::getVersion)))
          .toArray(Object[]::new));
    }

    private StackGresComponent getEnclosingInstance() {
      return StackGresComponent.this;
    }

    @Override
    public String toString() {
      return String.format("%s", versions);
    }
  }

  static class ImageVersion implements Comparable<ImageVersion> {

    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile(
        "^(?<version>(?<major>\\d+)"
            + "(?:\\.(?<minor>\\d+))?"
            + "(?:\\.(?<patch>\\d+))?"
            + "(?:-(?<suffix>(?:alpha|beta)(?<suffixversion>\\d+)))?)"
            + "(?:-build-(?<build>(?<buildmajor>\\d+)"
            + "(?:\\.(?<buildminor>\\d+)(?:-dev)?)?))$");

    final String version;
    final Integer major;
    final Integer minor;
    final Integer patch;
    final String suffix;
    final Integer versionType;
    final Integer suffixVersion;
    final String build;
    final Integer buildMajor;
    final Integer buildMinor;

    ImageVersion(String version) {
      Matcher matcher = IMAGE_TAG_PATTERN.matcher(version);
      Preconditions.checkArgument(matcher.find(),
          "Image tag " + version + " does not follow pattern "
              + IMAGE_TAG_PATTERN);
      this.version = matcher.group("version");
      this.major = Integer.parseInt(matcher.group("major"));
      this.minor = Optional.ofNullable(matcher.group("minor"))
          .map(Integer::parseInt).orElse(null);
      this.patch = Optional.ofNullable(matcher.group("patch"))
          .map(Integer::parseInt).orElse(null);
      this.suffix = matcher.group("suffix");
      this.versionType = Optional.ofNullable(this.suffix)
          .map(suffix -> suffix.equals("alpha") ? 0 : 1)
          .orElse(2);
      this.suffixVersion = Optional.ofNullable(matcher.group("suffixversion"))
          .map(Integer::parseInt).orElse(null);
      this.build = matcher.group("build");
      this.buildMajor = Integer.parseInt(matcher.group("buildmajor"));
      this.buildMinor = Optional.ofNullable(matcher.group("buildminor"))
          .map(Integer::parseInt).orElse(0);
    }

    public String getVersion() {
      return version;
    }

    public Integer getMajor() {
      return major;
    }

    public Integer getMinor() {
      return minor;
    }

    public Integer getPatch() {
      return patch;
    }

    public String getBuild() {
      return build;
    }

    public Integer getBuildMajor() {
      return buildMajor;
    }

    public Integer getBuildMinor() {
      return buildMinor;
    }

    @Override
    public int compareTo(ImageVersion o) {
      int compare = major.compareTo(o.major);
      if (compare == 0 && minor != null && o.minor != null) {
        compare = minor.compareTo(o.minor);
      }
      if (compare == 0 && patch != null && o.patch != null) {
        compare = patch.compareTo(o.patch);
      }
      if (compare == 0 && versionType != null && o.versionType != null) {
        compare = versionType.compareTo(o.versionType);
      }
      if (compare == 0 && suffixVersion != null && o.suffixVersion != null) {
        compare = suffixVersion.compareTo(o.suffixVersion);
      }
      if (compare == 0) {
        compare = buildMajor.compareTo(o.buildMajor);
      }
      if (compare == 0 && buildMinor != null && o.buildMinor != null) {
        compare = buildMinor.compareTo(o.buildMinor);
      }
      return compare;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hash(version, build);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ImageVersion)) {
        return false;
      }
      ImageVersion other = (ImageVersion) obj;
      return Objects.equals(version, other.version) && Objects.equals(build, other.build);
    }

    @Override
    public String toString() {
      return String.format("%s-build-%s", version, build);
    }
  }

  public enum VersionReader {

    INSTANCE;

    final ImmutableMap<String, String> componentVersions;

    VersionReader() {
      this.componentVersions = readComponentVersions();
    }

    @SuppressFBWarnings(value = "UI_INHERITANCE_UNSAFE_GETRESOURCE",
        justification = "It is the wanted behavior")
    ImmutableMap<String, String> readComponentVersions() {
      try (InputStream is = getClass().getResourceAsStream("/versions.properties")) {
        Properties properties = new Properties();
        properties.load(is);
        return Seq.seq(properties)
            .collect(ImmutableMap.toImmutableMap(
                t -> t.v1.toString(), t -> t.v2.toString()));
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }

    String get(StackGresComponent component) {
      return componentVersions.get(component.name);
    }

    String get(StackGresComponent component, int subComponent) {
      Preconditions.checkArgument(component.subComponents.size() > subComponent,
          "Component " + component.name + " does not delare a"
              + " subComponent with index " + subComponent);
      Preconditions.checkArgument(subComponent >= 0,
          "Invalid negative subComponent index " + subComponent
              + " for component " + component.name);
      return componentVersions.get(component.subComponents.get(subComponent).name);
    }

    String[] getAsArray(StackGresComponent component) {
      return get(component).split(ARRAY_SPLIT_REGEXP);
    }

    String[] getAsArray(StackGresComponent component, int subComponent) {
      return get(component, subComponent).split(ARRAY_SPLIT_REGEXP);
    }
  }

  public String findLatestImageName() {
    return findImageName(LATEST, Seq.seq(this.subComponents)
        .collect(ImmutableMap.toImmutableMap(
            Function.identity(), subComponent -> LATEST)));
  }

  public String findImageName(String version) {
    return findImageName(version, ImmutableMap.of());
  }

  public String findImageName(String version,
      Map<StackGresComponent, String> subComponentVersions) {
    checkSubComponents(subComponentVersions);
    return orderedComposedVersions()
        .filter(cv -> isVersion(version, cv.getVersion()))
        .filter(cv -> Seq.seq(subComponentVersions)
            .allMatch(t -> isVersion(t.v2, cv.getVersion(t.v1))))
        .map(ComposedVersion::getImageName)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " and sub-components "
                + subComponentVersions + " not available"));
  }

  public String findLatestVersion() {
    return findVersion(LATEST);
  }

  public String findVersion(String version) {
    return latestBuildVersion(version)
        .map(ImageVersion::getVersion)
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " not available"));
  }

  public String findMajorVersion(String version) {
    return latestBuildVersion(version)
        .map(ImageVersion::getMajor)
        .map(Object::toString)
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " not available"));
  }

  public String findBuildVersion(String version) {
    return latestBuildVersion(version)
        .map(ImageVersion::getBuild)
        .map(Object::toString)
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " not available"));
  }

  public String findBuildMajorVersion(String version) {
    return latestBuildVersion(version)
        .map(ImageVersion::getBuildMajor)
        .map(Object::toString)
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " not available"));
  }

  private Optional<ImageVersion> latestBuildVersion(String version) {
    return orderedVersions()
        .filter(v -> isVersion(version, v))
        .findFirst();
  }

  private boolean isVersion(String version, ImageVersion v) {
    return version == null
        || LATEST.equals(version)
        || v.getVersion().equals(version)
        || v.getVersion().startsWith(version + ".");
  }

  public Seq<String> getOrderedVersions() {
    return orderedVersions()
        .map(ImageVersion::getVersion);
  }

  public Seq<String> getOrderedVersions(String build) {
    return orderedVersions()
        .filter(imageVersion -> imageVersion.getBuild().equals(build))
        .map(ImageVersion::getVersion);
  }

  public Seq<String> getOrderedMajorVersions() {
    return orderedVersions()
        .map(ImageVersion::getMajor)
        .map(Object::toString)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedMajorVersions(String build) {
    return orderedVersions()
        .filter(imageVersion -> imageVersion.getBuild().equals(build))
        .map(ImageVersion::getMajor)
        .map(Object::toString)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedBuildVersions() {
    return orderedVersions()
        .map(ImageVersion::getBuild)
        .filter(Objects::nonNull)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedBuildMajorVersions() {
    return orderedVersions()
        .map(ImageVersion::getBuildMajor)
        .map(String::valueOf)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedImageNames() {
    return orderedComposedVersions()
        .map(ComposedVersion::getImageName);
  }

  private Seq<ImageVersion> orderedVersions() {
    return orderedComposedVersions()
        .map(ComposedVersion::getVersion)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  private Seq<ComposedVersion> orderedComposedVersions() {
    return Seq.seq(this.composedVersions())
        .sorted(Comparator.reverseOrder());
  }

  private void checkSubComponents(Map<StackGresComponent, String> subComponentVersions) {
    Preconditions.checkArgument(Seq.seq(this.subComponents)
        .allMatch(subComponentVersions::containsKey),
        "You must specify sub-component versions for "
            + Seq.seq(this.subComponents)
            .filter(subComponent -> !subComponentVersions.containsKey(subComponent))
            .toString(", "));
  }
}
