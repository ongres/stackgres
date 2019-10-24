/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.sidecars.pgexporter.customresources;

import io.stackgres.operator.common.StackGresUtil;

public enum StackGresPostgresExporterConfigDefinition {

  ;

  public static final String KIND = "StackGresPrometheusPostgresExporterConfig";
  public static final String SINGULAR = "sgprometheuspostgresexporterconfig";
  public static final String PLURAL = "sgprometheuspostgresexporterconfigs";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;

}
