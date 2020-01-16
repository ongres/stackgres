/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgrestoreconfig;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackgresRestoreConfigList extends CustomResourceList<StackgresRestoreConfig> {

  private static final long serialVersionUID = 1L;
}
