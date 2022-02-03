/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgobjectstorage;

import io.fabric8.kubernetes.client.CustomResourceList;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class StackGresObjectStorageList extends CustomResourceList<StackGresObjectStorage> {

  private static final long serialVersionUID = 1L;
}
