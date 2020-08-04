/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.apiweb.dto.Metadata;

public class MetadataMapper {

  public static Metadata map(ObjectMeta source) {
    Metadata metadata = new Metadata();
    metadata.setName(source.getName());
    metadata.setNamespace(source.getNamespace());
    metadata.setUid(source.getUid());
    return metadata;
  }
}
