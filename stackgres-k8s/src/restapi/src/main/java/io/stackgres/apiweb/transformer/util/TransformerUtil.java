/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.util;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.apiweb.dto.Metadata;

public interface TransformerUtil {

  static Metadata fromResource(ObjectMeta objectMeta) {
    Metadata metadata = new Metadata();
    metadata.setAnnotations(objectMeta.getAnnotations());
    metadata.setLabels(objectMeta.getLabels());
    metadata.setCreationTimestamp(objectMeta.getCreationTimestamp());
    metadata.setDeletionGracePeriodSeconds(objectMeta.getDeletionGracePeriodSeconds());
    metadata.setDeletionTimestamp(objectMeta.getDeletionTimestamp());
    metadata.setFinalizers(objectMeta.getFinalizers());
    metadata.setGenerateName(objectMeta.getGenerateName());
    metadata.setGeneration(objectMeta.getGeneration());
    metadata.setName(objectMeta.getName());
    metadata.setNamespace(objectMeta.getNamespace());
    metadata.setResourceVersion(objectMeta.getResourceVersion());
    metadata.setSelfLink(objectMeta.getSelfLink());
    metadata.setOwnerReferences(objectMeta.getOwnerReferences());
    metadata.setUid(objectMeta.getUid());
    return metadata;
  }

  static ObjectMeta fromDto(Metadata metadata, ObjectMeta originalObjectMeta) {
    ObjectMeta objectMeta = originalObjectMeta != null ? originalObjectMeta : new ObjectMeta();
    if (metadata.getAnnotations() != null) {
      objectMeta.setAnnotations(metadata.getAnnotations());
    }
    if (metadata.getLabels() != null) {
      objectMeta.setLabels(metadata.getLabels());
    }
    objectMeta.setNamespace(metadata.getNamespace());
    objectMeta.setName(metadata.getName());
    objectMeta.setUid(metadata.getUid());
    return objectMeta;
  }

}
