/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgrestoreconfig;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.common.StackGresUtil;

@RegisterForReflection
public enum StackgresRestoreConfigDefinition {

  ;

  public static final String KIND = "StackgresRestoreConfig";
  public static final String SINGULAR = "sgrestoreconfig";
  public static final String PLURAL = "sgrestoreconfigs";
  public static final String NAME = PLURAL + "." + StackGresUtil.CRD_GROUP;
  public static final String APIVERSION = StackGresUtil.CRD_GROUP + "/" + StackGresUtil.CRD_VERSION;
}
