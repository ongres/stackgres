/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.HasMetadata;

public record ResourceKindKey(
    String apiVersion,
    String kind) {

  public static ResourceKindKey create(HasMetadata resource) {
    return new ResourceKindKey(
        resource.getApiVersion(),
        resource.getKind());
  }

}
