/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.component.Component.ComponentVersion;
import io.stackgres.common.component.Component.ComposedComponentVersion;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public class PropertiesVersionReader implements VersionReader {

  static final String ARRAY_SPLIT_REGEXP = ",";

  final Map<String, String> componentVersions;
  final String prefix;
  final StackGresProperty imageTemplateProperty;
  final String defaultImageTemplate;
  final StackGresProperty componentVersionProperty;

  PropertiesVersionReader(
      Map<String, String> componentVersions,
      String prefix,
      StackGresProperty imageTemplateProperty,
      String defaultImageTemplate,
      StackGresProperty componentVersionProperty) {
    this.componentVersions = componentVersions;
    this.prefix = prefix;
    this.imageTemplateProperty = imageTemplateProperty;
    this.defaultImageTemplate = defaultImageTemplate;
    this.componentVersionProperty = componentVersionProperty;
  }

  static class ForFile {
    final Map<String, String> componentVersions;

    ForFile(String versionProperties) {
      this.componentVersions = readComponentVersions(versionProperties);
    }

    Map<String, String> readComponentVersions(String versionProperties) {
      try (InputStream is = getClass().getResourceAsStream(versionProperties)) {
        Properties properties = new Properties();
        properties.load(is);
        return Seq.seq(properties)
            .collect(Collectors.toMap(
                t -> t.v1.toString(), t -> t.v2.toString().replaceAll("[ \\n\\r]", "")));
      } catch (IOException ex) {
        throw new UncheckedIOException(ex);
      }
    }
    
  }

  @Override
  public List<ComposedComponentVersion> getComposedVersions(StackGresContext context, Component component) {
    return Seq.seq(component.getSubComponents())
        .map(alternativeSubComponents -> Seq.seq(alternativeSubComponents)
            .map(subComponentVersions(context, component)::get)
            .toList())
        .reduce(
            Seq.seq(versions(context, component))
            .map(version -> new PropertyComposedComponentVersion(component, version))
            .toList(),
            (composedVersions, subComponents) -> Seq.seq(subComponents)
                .zipWithIndex()
                .flatMap(alternativeSubComponents -> Seq.seq(alternativeSubComponents.v1)
                    .innerJoin(
                        Seq.seq(composedVersions),
                        (alternativeSubVersion, composedVersion) -> alternativeSubVersion.getBuild()
                        .equals(composedVersion.getVersion().getBuild()))
                    .map(t -> t.v2.append(alternativeSubComponents.v2.intValue(), t.v1)))
                .toList(),
            (u, v) -> v)
        .stream()
        .sorted(Comparator.reverseOrder())
        .map(ComposedComponentVersion.class::cast)
        .toList();
  }

  @Override
  public boolean hasImage() {
    return defaultImageTemplate != null;
  }

  public String getPrefix() {
    return prefix;
  }

  List<PropertyComponentVersion> versions(StackGresContext context, Component component) {
    return Optional.ofNullable(componentVersionProperty)
        .flatMap(StackGresProperty::get)
        .map(PropertyComponentVersion::new)
        .map(List::of)
        .orElseGet(() -> Seq.of(getAsArray(context, component))
            .map(PropertyComponentVersion::new)
            .toList());
  }

  Map<Component, List<PropertyComponentVersion>> subComponentVersions(StackGresContext context, Component component) {
    return Seq.range(0, component.getSubComponents().size())
                .flatMap(subComponentsIndex -> Seq
                    .range(0, component.getSubComponents().get(subComponentsIndex).size())
                    .map(subComponentAlternativeIndex -> Tuple
                        .tuple(subComponentsIndex, subComponentAlternativeIndex)))
                .map(t -> t.concat(
                    Seq.of(getAsArray(context, component, t.v1, t.v2))
                    .map(PropertyComponentVersion::new)
                    .toList()))
                .collect(Collectors.toMap(
                    t -> component.getSubComponents().get(t.v1).get(t.v2),
                    t -> t.v3));
  }

  String[] getAsArray(
      StackGresContext context,
      Component component) {
    return get(context, component).split(ARRAY_SPLIT_REGEXP);
  }

  String[] getAsArray(
      StackGresContext context,
      Component component,
      int subComponent,
      int alternativeSubComponent) {
    return get(context, component, subComponent, alternativeSubComponent).split(ARRAY_SPLIT_REGEXP);
  }

  String get(
      StackGresContext context,
      Component component) {
    return componentVersions.get(component.getName());
  }

  String get(
      StackGresContext context,
      Component component,
      int subComponent,
      int alternativeSubComponent) {
    Preconditions.checkArgument(component.subComponents.size() > subComponent,
        "Component " + component.getName() + " does not delare a"
            + " sub component with index " + subComponent);
    Preconditions.checkArgument(subComponent >= 0,
        "Invalid negative sub component index " + subComponent
            + " for component " + component.getName());
    List<Component> alternativeSubComponents =
        component.subComponents.get(subComponent);
    Preconditions.checkArgument(alternativeSubComponents.size() > alternativeSubComponent,
        "Component " + component.getName() + " does not delare a"
            + " alternative " + alternativeSubComponent
            + " under sub component " + subComponent);
    Preconditions.checkArgument(alternativeSubComponent >= 0,
        "Invalid negative alternative index " + alternativeSubComponent
            + " under sub component " + subComponent
            + " for component " + component.getName());
    return componentVersions.get(alternativeSubComponents.get(alternativeSubComponent).getName());
  }

  class PropertyComposedComponentVersion implements ComposedComponentVersion {
    final Component component;
    final ComponentVersion version;
    final List<Tuple2<Integer, ComponentVersion>> subVersions;

    public PropertyComposedComponentVersion(
        Component component,
        ComponentVersion version) {
      this.component = component;
      this.version = version;
      this.subVersions = List.of();
    }

    private PropertyComposedComponentVersion(
        ComposedComponentVersion composedVersion,
        Integer alternativeSubComponent,
        ComponentVersion subVersion) {
      this.component = composedVersion.getComponent();
      this.version = composedVersion.getVersion();
      this.subVersions = Seq.seq(composedVersion.getSubVersions())
          .append(Tuple.tuple(alternativeSubComponent, subVersion))
          .toList();
    }

    public PropertyComposedComponentVersion append(Integer alternativeSubComponent, ComponentVersion subVersion) {
      return new PropertyComposedComponentVersion(this, alternativeSubComponent, subVersion);
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
      return String.format(imageTemplate(),
          Seq.of(StackGresProperty.SG_CONTAINER_REGISTRY.getString())
          .append(Seq.of(getVersion().getVersion(), getVersion().getBuild())
            .append(Seq.seq(subVersions).zipWithIndex()
                .map(t -> Optional.ofNullable(
                        getComponent().getSubComponents()
                        .get(t.v2.intValue())
                        .get(t.v1.v1))
                    .map(Component::getVersionReader)
                    .filter(PropertiesVersionReader.class::isInstance)
                    .map(PropertiesVersionReader.class::cast)
                    .map(PropertiesVersionReader::getPrefix)
                    .orElse("") + t.v1.v2.getVersion())))
          .toArray(Object[]::new));
    }

    private String imageTemplate() {
      return Optional.ofNullable(imageTemplateProperty)
          .flatMap(StackGresProperty::get)
          .map(template -> template.replace("${containerRegistry}", "%1$s"))
          .map(template -> template.replace(
              "${" + getComponent().getName().replaceAll("[^a-z]", "") + "Version}", "%2$s"))
          .map(template -> template.replace("${buildVersion}", "%3$s"))
          .map(template -> Seq.seq(getComponent().getSubComponents())
              .zipWithIndex()
              .reduce(template, (templateResult, t) -> templateResult
                  .replace("${" + t.v1.get(0).getName().replaceAll("[^a-z]", "") + "Version}",
                      "%" + (t.v2 + 4) + "$s"),
                  (u, v) -> v))
          .orElse(Optional.ofNullable(defaultImageTemplate).orElseThrow());
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
      if (!(obj instanceof PropertyComposedComponentVersion)) {
        return false;
      }
      PropertyComposedComponentVersion other = (PropertyComposedComponentVersion) obj;
      return Objects.equals(component, other.component)
          && Objects.equals(subVersions, other.subVersions)
          && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
      return String.format("%s %s", version, subVersions);
    }
  }

  static class PropertyComponentVersion implements ComponentVersion {

    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile(
        "^(?<version>(?<major>\\d+)"
            + "(?:\\.(?<minor>\\d+))?"
            + "(?:\\.(?<patch>\\d+)[^0-9-]*)?"
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

    PropertyComponentVersion(String version) {
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
    public String getBuild() {
      return build;
    }

    @Override
    public String getBaseName() {
      return null;
    }

    @Override
    public Integer getBaseMajor() {
      return buildMajor;
    }

    @Override
    public Integer getBaseMinor() {
      return buildMinor;
    }

    @Override
    public Integer getRevision() {
      return null;
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
      if (!(obj instanceof PropertyComponentVersion)) {
        return false;
      }
      PropertyComponentVersion other = (PropertyComponentVersion) obj;
      return Objects.equals(version, other.version) && Objects.equals(build, other.build);
    }

    @Override
    public String toString() {
      return String.format("%s-build-%s", version, build);
    }

  }

}
