/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpgconfig;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackGresPostgresConfigList extends CustomResourceList<StackGresPostgresConfig> {

  private static final long serialVersionUID = -5276087851826599719L;

}
