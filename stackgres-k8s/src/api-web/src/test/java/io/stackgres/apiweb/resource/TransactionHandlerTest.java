/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.resource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.resource.ResourceWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public abstract class TransactionHandlerTest<R extends HasMetadata> {

  @Mock
  protected ResourceWriter<R> writer;

  protected ResourceTransactionHandler<R> transactionHandler;

  protected R resource = getResource();

  protected abstract ResourceTransactionHandler<R> getInstance();

  protected abstract R getResource();


  @BeforeEach
  void setUp() {
    transactionHandler = getInstance();
  }

  @Test
  void successfulTransaction_shouldNotCallRollback() {

    when(writer.create(resource)).thenReturn(resource);

    AtomicBoolean transactionCalled = new AtomicBoolean(false);

    transactionHandler.create(resource, () -> {
      transactionCalled.set(true);
    });

    assertTrue(transactionCalled.get());
    verify(writer).create(resource);
    verify(writer, never()).delete(any());
  }

  @Test
  void failedTransaction_shouldCallRollback() {

    when(writer.create(resource)).thenReturn(resource);
    doNothing().when(writer).delete(resource);

    assertThrows(RuntimeException.class, () -> transactionHandler.create(resource, () -> {
      throw new RuntimeException("a failure");
    }));

    verify(writer).create(resource);
    verify(writer).delete(resource);

  }

  @Test
  void failedCreation_shouldNotExecuteTheTransaction() {

    doThrow(new RuntimeException("a failure")).when(writer).create(resource);

    AtomicBoolean transactionCalled = new AtomicBoolean(false);

    assertThrows(RuntimeException.class, () -> transactionHandler
        .create(resource, () -> transactionCalled.set(true)));

    assertFalse(transactionCalled.get());
    verify(writer).create(resource);
    verify(writer, never()).delete(resource);

  }
}
