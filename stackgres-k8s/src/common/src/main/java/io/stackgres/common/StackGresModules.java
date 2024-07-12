/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import com.google.common.collect.ImmutableList;
import org.jooq.lambda.Seq;

public enum StackGresModules {

  CLUSTER_CONTROLLER("cluster-controller",
      StackGresProperty.SG_IMAGE_CLUSTER_CONTROLLER,
      StackGresProperty.OPERATOR_IMAGE_VERSION,
      "%1$s/stackgres/cluster-controller:%2$s"),
  DISTRIBUTEDLOGS_CONTROLLER("distributedlogs-controller",
      StackGresProperty.SG_IMAGE_DISTRIBUTEDLOGS_CONTROLLER,
      StackGresProperty.OPERATOR_IMAGE_VERSION,
      "%1$s/stackgres/distributedlogs-controller:%2$s"),
  STREAM("stream",
      StackGresProperty.SG_IMAGE_STREAM,
      StackGresProperty.OPERATOR_IMAGE_VERSION,
      "%1$s/stackgres/stream:%2$s");

  final String name;
  final StackGresProperty imageTemplateProperty;
  final String defaultImageTemplate;
  final StackGresProperty componentVersionProperty;
  final ImmutableList<StackGresModules> subComponents;

  StackGresModules(String name, StackGresProperty imageTemplateProperty,
      StackGresProperty componentVersionProperty,
      String defaultImageTemplate, StackGresModules...subComponents) {
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

  private String version() {
    return componentVersionProperty.get()
        .orElseThrow(() -> new RuntimeException("No version defined for controller " + name));
  }

  public String getVersion() {
    return version();
  }

  public String getImageName() {
    return String.format(imageTemplate(),
        Seq.of(StackGresProperty.SG_CONTAINER_REGISTRY.getString(), version())
        .toArray(Object[]::new));
  }
}
