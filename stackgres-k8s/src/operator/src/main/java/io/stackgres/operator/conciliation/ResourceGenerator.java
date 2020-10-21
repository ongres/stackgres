/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceGenerator<T> {

  Stream<HasMetadata> generateResource(T config);

}
