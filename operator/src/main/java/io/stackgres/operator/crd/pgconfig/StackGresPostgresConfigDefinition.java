/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.crd.pgconfig;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;

public class StackGresPostgresConfigDefinition {

  public static final String GROUP = "stackgres.io";
  public static final String VERSION = "v1alpha1";
  public static final String KIND = "StackGresPostgresConfig";
  public static final String SINGULAR = "sgpgconfig";
  public static final String PLURAL = "sgpgconfigs";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/" + VERSION;

  public static final CustomResourceDefinition CR_DEFINITION =
      new CustomResourceDefinitionBuilder()
          .withApiVersion("apiextensions.k8s.io/v1beta1")
          .withNewMetadata()
          .withName(NAME)
          .endMetadata()
          .withNewSpec()
          .withGroup(GROUP)
          .withVersion(VERSION)
          .withScope("Namespaced")
          .withNewNames()
          .withKind(KIND)
          .withListKind(KIND + "List")
          .withSingular(SINGULAR)
          .withPlural(PLURAL)
          .endNames()
          // .withValidation(getSchemaValidation())
          .endSpec()
          .build();

  private StackGresPostgresConfigDefinition() {
    throw new AssertionError("No instances for you!");
  }

//  private static CustomResourceValidation getSchemaValidation() {
//    // Bug: https://github.com/fabric8io/kubernetes-client/issues/1486
//
//    Map<String, JSONSchemaProps> properties = new HashMap<>();
//    properties.putIfAbsent("pg_version", new JSONSchemaPropsBuilder()
//        .withType("integer")
//        .withMinimum(11d)
//        .build());
//    properties.putIfAbsent("postgresql.conf", new JSONSchemaPropsBuilder()
//        .withType("object")
//        .build());
//
//    Map<String, JSONSchemaProps> spec = new HashMap<>();
//    spec.putIfAbsent("spec", new JSONSchemaPropsBuilder()
//        .withRequired(properties.keySet().stream().collect(Collectors.toList()))
//        .withProperties(properties)
//        .build());
//
//    JSONSchemaProps schema = new JSONSchemaPropsBuilder()
//        .withRequired("spec")
//        .withProperties(spec)
//        .build();
//
//    return new CustomResourceValidationBuilder()
//        .withOpenAPIV3Schema(schema)
//        .build();
//  }

}
