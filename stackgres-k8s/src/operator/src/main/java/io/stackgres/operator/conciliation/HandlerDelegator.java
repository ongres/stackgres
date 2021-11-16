/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;

public interface HandlerDelegator<T extends CustomResource<?, ?>> {

  HasMetadata create(T context, HasMetadata resource);

  HasMetadata patch(T context, HasMetadata newResource, HasMetadata oldResource);

  HasMetadata replace(T context, HasMetadata resource);

  void delete(T context, HasMetadata resource);

}
