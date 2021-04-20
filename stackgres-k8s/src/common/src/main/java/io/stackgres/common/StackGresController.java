/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import org.jooq.lambda.Seq;

public enum StackGresController {

  CLUSTER_CONTROLLER("cluster-controller",
      StackGresProperty.SG_IMAGE_CLUSTER_CONTROLLER,
      StackGresProperty.OPERATOR_IMAGE_VERSION,
      "%1$s/stackgres/cluster-controller:%2$s"),
  DISTRIBUTEDLOGS_CONTROLLER("distributedlogs-controller",
      StackGresProperty.SG_IMAGE_DISTRIBUTEDLOGS_CONTROLLER,
      StackGresProperty.OPERATOR_IMAGE_VERSION,
      "%1$s/stackgres/distributedlogs-controller:%2$s");

  private static final String CONTAINER_REGISTRY =
      StackGresProperty.SG_CONTAINER_REGISTRY.getString();

  final String name;
  final String imageTemplate;
  final String version;

  StackGresController(String name, StackGresProperty imageTemplateProperty,
      StackGresProperty componentVersionProperty,
      String defaultImageTemplate, StackGresController...subComponents) {
    this.name = name;
    this.imageTemplate = imageTemplateProperty.get()
        .map(template -> template.replace("${containerRegistry}", "%1$s"))
        .map(template -> template.replace(
            "${" + name.replaceAll("[^a-z]", "") + "Version}", "%2$s"))
        .map(template -> template.replace("${buildVersion}", "%3$s"))
        .map(template -> Seq.of(subComponents)
            .zipWithIndex()
            .reduce(template, (templateResult, t) -> templateResult
                .replace("${" + t.v1.name.replaceAll("[^a-z]", "") + "Version}",
                    "%" + (t.v2 + 4) + "$s"),
                (u, v) -> v))
        .orElse(defaultImageTemplate);
    this.version = componentVersionProperty.get()
        .orElseThrow(() -> new RuntimeException("No version defined for controller " + name));
  }

  public String getVersion() {
    return version;
  }

  public String getImageName() {
    return String.format(imageTemplate,
        Seq.of(CONTAINER_REGISTRY, version)
        .toArray(Object[]::new));
  }
}
