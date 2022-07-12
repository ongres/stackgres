/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgscript;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public final class StackGresScriptList extends CustomResourceList<StackGresScript> {

  private static final long serialVersionUID = -1L;

}
