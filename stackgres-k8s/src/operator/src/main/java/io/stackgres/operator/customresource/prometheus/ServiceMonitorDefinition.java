/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.prometheus;

public enum ServiceMonitorDefinition {

  ;

  public static final String GROUP = "monitoring.coreos.com";
  public static final String KIND = "ServiceMonitor";
  public static final String SINGULAR = "servicemonitor";
  public static final String PLURAL = "servicemonitors";
  public static final String NAME = PLURAL + "." + GROUP;
  public static final String APIVERSION = GROUP + "/v1";

}
