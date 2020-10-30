/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.prometheus;

import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

public class PrometheusConfigDefinition {
  ;

  public static final String GROUP = "monitoring.coreos.com";
  public static final String KIND = "Prometheus";
  public static final String SINGULAR = "prometheus";
  public static final String PLURAL = "prometheuses";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String VERSION = "v1";
  public static final String APIVERSION = GROUP + "/" + VERSION;
  public static final String SCOPE = "Namespaced";

  public static final CustomResourceDefinitionContext CONTEXT =
      new CustomResourceDefinitionContext.Builder()
      .withGroup(GROUP)
      .withVersion(VERSION)
      .withKind(KIND)
      .withPlural(PLURAL)
      .withName(NAME)
      .withScope(SCOPE)
      .build();
}
