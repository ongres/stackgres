/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ReconciliationHandler {

  HasMetadata create(HasMetadata resource);

  HasMetadata patch(HasMetadata newResource, HasMetadata oldResource);

  HasMetadata replace(HasMetadata resource);

  void delete(HasMetadata resource);
}
