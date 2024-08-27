/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jooq.lambda.Seq;

public class VersionReader {

  static final String ARRAY_SPLIT_REGEXP = ",";

  final ImmutableMap<String, String> componentVersions;

  VersionReader(String versionProperties) {
    this.componentVersions = readComponentVersions(versionProperties);
  }

  @SuppressFBWarnings(value = "UI_INHERITANCE_UNSAFE_GETRESOURCE",
      justification = "It is the wanted behavior")
  ImmutableMap<String, String> readComponentVersions(String versionProperties) {
    try (InputStream is = getClass().getResourceAsStream(versionProperties)) {
      Properties properties = new Properties();
      properties.load(is);
      return Seq.seq(properties)
          .collect(ImmutableMap.toImmutableMap(
              t -> t.v1.toString(), t -> t.v2.toString().replaceAll("[ \\n\\r]", "")));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  String get(Component component) {
    return componentVersions.get(component.name);
  }

  String get(Component component, int subComponent, int alternativeSubComponent) {
    Preconditions.checkArgument(component.subComponents.size() > subComponent,
        "Component " + component.name + " does not delare a"
            + " sub component with index " + subComponent);
    Preconditions.checkArgument(subComponent >= 0,
        "Invalid negative sub component index " + subComponent
            + " for component " + component.name);
    List<Component> alternativeSubComponents =
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

  String[] getAsArray(Component component) {
    return get(component).split(ARRAY_SPLIT_REGEXP);
  }

  String[] getAsArray(Component component, int subComponent,
      int alternativeSubComponent) {
    return get(component, subComponent, alternativeSubComponent).split(ARRAY_SPLIT_REGEXP);
  }
}
