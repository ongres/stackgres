/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.transformer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.rest.dto.Metadata;
import io.stackgres.operator.rest.dto.ResourceDto;

public abstract class AbstractResourceTransformer<T extends ResourceDto, R extends CustomResource>
    implements ResourceTransformer<T, R> {

  protected ObjectMeta getCustomResourceMetadata(T source) {
    ObjectMeta metadata = new ObjectMeta();
    metadata.setNamespace(source.getMetadata().getNamespace());
    metadata.setName(source.getMetadata().getName());
    return metadata;
  }

  protected Metadata getResourceMetadata(R source) {
    Metadata metadata = new Metadata();
    metadata.setNamespace(source.getMetadata().getNamespace());
    metadata.setName(source.getMetadata().getName());
    return metadata;
  }

}
