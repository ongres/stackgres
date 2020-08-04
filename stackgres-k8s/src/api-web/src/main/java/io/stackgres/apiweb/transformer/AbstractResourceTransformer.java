/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.apiweb.dto.ResourceDto;

public abstract class AbstractResourceTransformer<T extends ResourceDto, R extends CustomResource>
    extends AbstractDtoTransformer<T, R> implements ResourceTransformer<T, R> {

  public ObjectMeta getCustomResourceMetadata(T source, R original) {
    ObjectMeta metadata = original != null ? original.getMetadata() : new ObjectMeta();
    if (source.getMetadata() != null) {
      metadata.setNamespace(source.getMetadata().getNamespace());
      metadata.setName(source.getMetadata().getName());
      metadata.setUid(source.getMetadata().getUid());
    }
    return metadata;
  }

}
