/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.rest.dto.Resource;

public interface ResourceTransformer<T extends Resource, R extends CustomResource> {

  R toCustomResource(T resource);

  T toResource(R customResource);

}
