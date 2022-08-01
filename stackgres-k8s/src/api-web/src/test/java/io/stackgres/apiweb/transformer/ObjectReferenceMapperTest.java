/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.apiweb.dto.event.ObjectReference;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObjectReferenceMapperTest {

  private Event event;

  @BeforeEach
  void setup() {
    this.event = Fixtures.event().loadDefault().get();
  }

  @Test
  void shouldPopulatedObjectReference_onceInvolvedObjectIsAValidValue() {
    ObjectReference reference = ObjectReferenceMapper.map(event.getInvolvedObject());
    assertObjectReferenceIsSuccessfullyPopulated(event.getInvolvedObject(), reference);
  }

  @Test
  void shouldRetrieveAnEmptyInvolvedObject_onceEventInvolvedObjectHasNoValue() {
    event.setInvolvedObject(null);
    ObjectReference reference = ObjectReferenceMapper.map(event.getInvolvedObject());
    assertNull(reference);
  }

  @Test
  void shouldPopulateRelatedObject_onceEventRelatedisAValidValue() {
    ObjectReference reference = ObjectReferenceMapper.map(event.getRelated());
    assertObjectReferenceIsSuccessfullyPopulated(event.getRelated(), reference);
  }

  @Test
  void shouldRetrieveAnEmptyRelatedObject_onceEventInvolvedObjectHasNoValue() {
    event.setRelated(null);
    ObjectReference reference = ObjectReferenceMapper.map(event.getRelated());
    assertNull(reference);
  }

  private void assertObjectReferenceIsSuccessfullyPopulated(
      io.fabric8.kubernetes.api.model.ObjectReference objectReference,
      ObjectReference actualReference) {
    assertEquals(objectReference.getName(), actualReference.getName());
    assertEquals(objectReference.getKind(), actualReference.getKind());
    assertEquals(objectReference.getNamespace(), actualReference.getNamespace());
    assertEquals(objectReference.getUid(), actualReference.getUid());
  }

}
