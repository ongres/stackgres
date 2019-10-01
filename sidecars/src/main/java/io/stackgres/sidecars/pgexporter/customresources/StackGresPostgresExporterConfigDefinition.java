/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars.pgexporter.customresources;

import io.stackgres.common.StackGresUtil;

public enum StackGresPostgresExporterConfigDefinition {

  ;

  public static final String KIND = "StackGresPrometheusPostgresExporterConfig";
  public static final String SINGULAR = "sgprometheuspostgresexporterconfig";
  public static final String PLURAL = "sgprometheuspostgresexporterconfigs";
  public static final String NAME = PLURAL + "." + StackGresUtil.GROUP;
  public static final String APIVERSION = StackGresUtil.GROUP + "/" + StackGresUtil.CRD_VERSION;

}
