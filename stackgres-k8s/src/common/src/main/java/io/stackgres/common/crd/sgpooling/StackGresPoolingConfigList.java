/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgpooling;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public final class StackGresPoolingConfigList extends CustomResourceList<StackGresPoolingConfig> {

  private static final long serialVersionUID = -1986325130709722399L;

}
