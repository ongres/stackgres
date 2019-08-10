/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.crd.sgprofile;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;

public class StackGresProfileDefinition {

  public static final String GROUP = "stackgres.io";
  public static final String VERSION = "v1alpha1";
  public static final String KIND = "StackGresProfile";
  public static final String SINGULAR = "sgprofile";
  public static final String PLURAL = "sgprofiles";
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

  private StackGresProfileDefinition() {
    throw new AssertionError("No instances for you!");
  }

}
