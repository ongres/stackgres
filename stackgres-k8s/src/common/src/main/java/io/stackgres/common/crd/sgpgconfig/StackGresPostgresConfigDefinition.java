/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.stackgres.common.StackGresProperty;

public enum StackGresPostgresConfigDefinition {

  ;

  public static final String KIND = "SGPostgresConfig";
  public static final String SINGULAR = "sgpgconfig";
  public static final String PLURAL = "sgpgconfigs";
  public static final String NAME = PLURAL + "." + StackGresProperty.CRD_GROUP.getString();
  public static final String APIVERSION = StackGresProperty.CRD_GROUP.getString()
      + "/" + StackGresProperty.CRD_VERSION.getString();
  public static final String SCOPE = "Namespaced";

  public static final CustomResourceDefinitionContext CONTEXT =
      new CustomResourceDefinitionContext.Builder()
      .withGroup(StackGresProperty.CRD_GROUP.getString())
      .withVersion(StackGresProperty.CRD_VERSION.getString())
      .withKind(KIND)
      .withPlural(PLURAL)
      .withName(NAME)
      .withScope(SCOPE)
      .build();

}
