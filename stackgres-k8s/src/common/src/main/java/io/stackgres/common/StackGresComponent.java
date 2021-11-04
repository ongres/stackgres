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
import org.jooq.lambda.tuple.Tuple2;

public enum StackGresComponent {

  POSTGRESQL("postgresql", "pg"),
  BABELFISH("babelfish", "bf"),
  PATRONI("patroni", StackGresProperty.SG_IMAGE_PATRONI,
      "%1$s/ongres/patroni:v%2$s-%4$s-build-%3$s",
      new StackGresComponent[] {
          StackGresComponent.POSTGRESQL,
          StackGresComponent.BABELFISH,
      }),
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
  final String prefix;
  final StackGresProperty imageTemplateProperty;
  final String defaultImageTemplate;
  final StackGresProperty componentVersionProperty;
  final List<List<StackGresComponent>> subComponents;

  StackGresComponent(String name, String prefix) {
    this(name, prefix, null, null, null);
  }

  StackGresComponent(String name, StackGresProperty imageTemplateProperty,
      String defaultImageTemplate, StackGresComponent[]...subComponents) {
    this(name, null, imageTemplateProperty, null, defaultImageTemplate, subComponents);
  }

  StackGresComponent(String name, String prefix, StackGresProperty imageTemplateProperty,
      StackGresProperty componentVersionProperty,
      String defaultImageTemplate, StackGresComponent[]...subComponents) {
    this.name = name;
    this.prefix = prefix;
    this.imageTemplateProperty = imageTemplateProperty;
    this.defaultImageTemplate = defaultImageTemplate;
    this.componentVersionProperty = componentVersionProperty;
    this.subComponents = Seq.of(subComponents)
        .map(subComponentArray -> ImmutableList.copyOf(subComponentArray))
        .collect(ImmutableList.toImmutableList());
  }

  public String getName() {
    return name;
  }

  public List<List<StackGresComponent>> getSubComponents() {
    return subComponents;
  }

  public boolean hasImage() {
    return defaultImageTemplate != null;
  }

  private String imageTemplate() {
    return Optional.ofNullable(imageTemplateProperty)
        .flatMap(StackGresProperty::get)
        .map(template -> template.replace("${containerRegistry}", "%1$s"))
        .map(template -> template.replace(
            "${" + name.replaceAll("[^a-z]", "") + "Version}", "%2$s"))
        .map(template -> template.replace("${buildVersion}", "%3$s"))
        .map(template -> Seq.seq(subComponents)
            .zipWithIndex()
            .reduce(template, (templateResult, t) -> templateResult
                .replace("${" + t.v1.get(0).name.replaceAll("[^a-z]", "") + "Version}",
                    "%" + (t.v2 + 4) + "$s"),
                (u, v) -> v))
        .orElse(Optional.ofNullable(defaultImageTemplate).orElseThrow());
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

  public List<ComposedVersion> getComposedVersions() {
    return Seq.seq(this.subComponents)
        .map(alternativeSubComponents -> Seq.seq(alternativeSubComponents)
            .map(subComponentVersions()::get)
            .toList())
        .<List<ComposedVersion>>reduce(
            Seq.seq(versions()).map(ComposedVersion::new).toList(),
            (composedVersions, subComponents) -> Seq.seq(subComponents)
                .zipWithIndex()
                .flatMap(alternativeSubComponents -> Seq.seq(alternativeSubComponents.v1)
                    .innerJoin(Seq.seq(composedVersions),
                      (alternativeSubVersion, composedVersion) -> alternativeSubVersion.build
                      .equals(composedVersion.getVersion().build))
                    .map(t -> t.v2.append(alternativeSubComponents.v2.intValue(), t.v1)))
                .toList(),
            (u, v) -> v)
        .stream()
        .sorted(Comparator.reverseOrder())
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableMap<StackGresComponent, List<ImageVersion>> subComponentVersions() {
    return Seq.range(0, subComponents.size())
                .flatMap(subComponents -> Seq.range(0, this.subComponents.get(subComponents).size())
                    .map(subComponent -> Tuple.tuple(subComponents, subComponent)))
                .map(t -> t.concat(
                    Seq.of(VersionReader.INSTANCE.getAsArray(this, t.v1, t.v2))
                    .map(ImageVersion::new)
                    .collect(ImmutableList.toImmutableList())))
                .collect(ImmutableMap.toImmutableMap(
                    t -> subComponents.get(t.v1).get(t.v2),
                    t -> t.v3));
  }

  public class ComposedVersion implements Comparable<ComposedVersion> {
    final ImageVersion version;
    final List<Tuple2<Integer, ImageVersion>> subVersions;

    public ComposedVersion(ImageVersion version) {
      this.version = version;
      this.subVersions = ImmutableList.of();
    }

    private ComposedVersion(ComposedVersion composedVersion, Integer alternativeSubComponent,
        ImageVersion subVersion) {
      this.version = composedVersion.version;
      this.subVersions = Seq.seq(composedVersion.subVersions)
          .append(Tuple.tuple(alternativeSubComponent, subVersion))
          .collect(ImmutableList.toImmutableList());
    }

    public ComposedVersion append(Integer alternativeSubComponent, ImageVersion subVersion) {
      return new ComposedVersion(this, alternativeSubComponent, subVersion);
    }

    public ImageVersion getVersion() {
      return version;
    }

    public List<Tuple2<Integer, ImageVersion>> getSubVersions() {
      return subVersions;
    }

    @Override
    public int compareTo(ComposedVersion o) {
      int compare = version.compareTo(o.version);
      int index = 0;
      while (compare == 0 && index < subVersions.size()) {
        compare = subVersions.get(index).compareTo(o.subVersions.get(index));
        index++;
      }
      return compare;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getEnclosingInstance().hashCode();
      result = prime * result + Objects.hash(subVersions, version);
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
      return Objects.equals(subVersions, other.subVersions)
          && Objects.equals(version, other.version);
    }

    public String getImageName() {
      return String.format(StackGresComponent.this.imageTemplate(),
          Seq.of(StackGresProperty.SG_CONTAINER_REGISTRY.getString())
          .append(Seq.of(getVersion().getVersion(), getVersion().getBuild())
            .append(Seq.seq(subVersions).zipWithIndex()
                .map(t -> Optional.ofNullable(
                        subComponents
                        .get(t.v2.intValue())
                        .get(t.v1.v1)
                        .prefix)
                    .orElse("") + t.v1.v2.getVersion())))
          .toArray(Object[]::new));
    }

    private StackGresComponent getEnclosingInstance() {
      return StackGresComponent.this;
    }

    @Override
    public String toString() {
      return String.format("%s %s", version, subVersions);
    }
  }

  public static class ImageVersion implements Comparable<ImageVersion> {

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

    String get(StackGresComponent component, int subComponent, int alternativeSubComponent) {
      Preconditions.checkArgument(component.subComponents.size() > subComponent,
          "Component " + component.name + " does not delare a"
              + " sub component with index " + subComponent);
      Preconditions.checkArgument(subComponent >= 0,
          "Invalid negative sub component index " + subComponent
              + " for component " + component.name);
      List<StackGresComponent> alternativeSubComponents =
          component.subComponents.get(subComponent);
      Preconditions.checkArgument(alternativeSubComponents.size() > alternativeSubComponent,
          "Component " + component.name + " does not delare a"
              + " alternative " + alternativeSubComponent
              + " under sub component " + subComponent);
      Preconditions.checkArgument(alternativeSubComponent >= 0,
          "Invalid negative alternative index " + alternativeSubComponent
              + " under sub component " + subComponent
              + " for component " + component.name);
      return componentVersions.get(alternativeSubComponents.get(alternativeSubComponent).name);
    }

    String[] getAsArray(StackGresComponent component) {
      return get(component).split(ARRAY_SPLIT_REGEXP);
    }

    String[] getAsArray(StackGresComponent component, int subComponent,
        int alternativeSubComponent) {
      return get(component, subComponent, alternativeSubComponent).split(ARRAY_SPLIT_REGEXP);
    }
  }

  public String findLatestImageName() {
    return findImageName(LATEST, Seq.seq(this.subComponents)
        .map(alternativeSubComponents -> alternativeSubComponents.get(0))
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
        .filter(cv -> Seq.seq(cv.getSubVersions())
            .zipWithIndex()
            .map(subVersion -> Tuple.tuple(
                subComponents.get(subVersion.v2.intValue()).get(subVersion.v1.v1),
                subVersion.v1.v2))
            .allMatch(subVersion -> subComponentVersions.containsKey(subVersion.v1)
                && isVersion(subComponentVersions.get(subVersion.v1), subVersion.v2)))
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

  public String findLatestMajorVersion() {
    return findMajorVersion(LATEST);
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

  public String findBuildVersion(String version,
      Map<StackGresComponent, String> subComponentVersions) {
    checkSubComponents(subComponentVersions);
    return orderedComposedVersions()
        .filter(cv -> isVersion(version, cv.getVersion()))
        .filter(cv -> Seq.seq(cv.getSubVersions())
            .zipWithIndex()
            .map(subVersion -> Tuple.tuple(
                subComponents.get(subVersion.v2.intValue()).get(subVersion.v1.v1),
                subVersion.v1.v2))
            .allMatch(subVersion -> subComponentVersions.containsKey(subVersion.v1)
                && isVersion(subComponentVersions.get(subVersion.v1), subVersion.v2)))
        .map(ComposedVersion::getVersion)
        .map(ImageVersion::getBuild)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " and sub-components "
                + subComponentVersions + " not available"));
  }

  public String findBuildMajorVersion(String version) {
    return latestBuildVersion(version)
        .map(ImageVersion::getBuildMajor)
        .map(Object::toString)
        .orElseThrow(() -> new IllegalArgumentException(
            this.name + " version " + version + " not available"));
  }

  private Optional<ImageVersion> latestBuildVersion(String version) {
    return orderedTagVersions()
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
    return orderedTagVersions()
        .map(ImageVersion::getVersion)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedVersions(String build) {
    return orderedTagVersions()
        .filter(imageVersion -> imageVersion.getBuild().equals(build))
        .map(ImageVersion::getVersion)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedMajorVersions() {
    return orderedTagVersions()
        .map(ImageVersion::getMajor)
        .map(Object::toString)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedMajorVersions(String build) {
    return orderedTagVersions()
        .filter(imageVersion -> imageVersion.getBuild().equals(build))
        .map(ImageVersion::getMajor)
        .map(Object::toString)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedBuildVersions() {
    return orderedTagVersions()
        .map(ImageVersion::getBuild)
        .filter(Objects::nonNull)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedBuildMajorVersions() {
    return orderedTagVersions()
        .map(ImageVersion::getBuildMajor)
        .map(String::valueOf)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<String> getOrderedImageNames() {
    return orderedComposedVersions()
        .map(ComposedVersion::getImageName);
  }

  public Seq<ImageVersion> orderedTagVersions() {
    return orderedComposedVersions()
        .map(ComposedVersion::getVersion)
        .grouped(Function.identity())
        .map(t -> t.v1);
  }

  public Seq<ComposedVersion> orderedComposedVersions() {
    return Seq.seq(getComposedVersions());
  }

  private void checkSubComponents(Map<StackGresComponent, String> subComponentVersions) {
    Preconditions.checkArgument(Seq.seq(this.subComponents)
        .allMatch(alternativeSubComponents -> alternativeSubComponents.stream()
            .anyMatch(subComponentVersions::containsKey)),
        "You must specify sub component versions for "
            + Seq.seq(this.subComponents)
            .filter(alternativeSubComponents -> alternativeSubComponents.stream()
                .noneMatch(subComponentVersions::containsKey))
            .map(alternativeSubComponent -> Seq.seq(alternativeSubComponent).toString(" or "))
            .toString(", "));
  }
}
