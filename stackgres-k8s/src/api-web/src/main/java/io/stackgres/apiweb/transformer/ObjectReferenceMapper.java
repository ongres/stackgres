/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.stackgres.apiweb.dto.event.ObjectReference;

public class ObjectReferenceMapper {

  public static ObjectReference map(io.fabric8.kubernetes.api.model.ObjectReference reference) {
    ObjectReference objectReference = new ObjectReference();

    if (reference == null) {
      return objectReference;
    }
    objectReference.setKind(reference.getKind());
    objectReference.setNamespace(reference.getNamespace());
    objectReference.setName(reference.getName());
    objectReference.setUid(reference.getUid());
    return objectReference;
  }

}
