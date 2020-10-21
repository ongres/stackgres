/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import io.fabric8.kubernetes.api.model.OwnerReference;

public interface OwnerReferenceFactory<T> {

  List<OwnerReference> buildOwnerFactory(T context);
}
