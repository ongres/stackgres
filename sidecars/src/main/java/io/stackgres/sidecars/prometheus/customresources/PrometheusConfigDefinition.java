/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.prometheus.customresources;

public class PrometheusConfigDefinition {
  ;

  public static final String GROUP = "monitoring.coreos.com";
  public static final String KIND = "Prometheus";
  public static final String SINGULAR = "prometheus";
  public static final String PLURAL = "prometheuses";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/v1";
}
