/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface Decorator<T> {

  HasMetadata decorate(T context, HasMetadata resource);

}
