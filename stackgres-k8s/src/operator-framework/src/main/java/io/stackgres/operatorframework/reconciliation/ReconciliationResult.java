/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import org.jooq.lambda.Seq;

public class ReconciliationResult<R> {
  private final R result;
  private final ImmutableList<Exception> exceptions;

  @SuppressWarnings("null")
  public ReconciliationResult() {
    this((R) null);
  }

  public ReconciliationResult(R result) {
    this(result, ImmutableList.of());
  }

  public ReconciliationResult(Exception exception) {
    this(null, exception);
  }

  public ReconciliationResult(R result,
      Exception exception) {
    this(result, ImmutableList.of(exception));
  }

  public ReconciliationResult(
      ImmutableList<Exception> exceptions) {
    this(null, exceptions);
  }

  public ReconciliationResult(R result,
      ImmutableList<Exception> exceptions) {
    this.result = result;
    this.exceptions = exceptions;
  }

  public Optional<R> result() {
    return Optional.ofNullable(result);
  }

  public ImmutableList<Exception> getExceptions() {
    return exceptions;
  }

  public boolean success() {
    return exceptions.isEmpty();
  }

  public ReconciliationResult<Void> join(ReconciliationResult<?> other) {
    return new ReconciliationResult<Void>(null,
        Seq.seq(exceptions)
        .append(other.exceptions)
        .collect(ImmutableList.toImmutableList()));
  }

  public Exception getException() {
    if (!exceptions.isEmpty()) {
      final Exception exception = exceptions.get(0);
      Seq.seq(exceptions)
          .skip(1)
          .forEach(suppressedException -> exception.addSuppressed(suppressedException));
      return exception;
    }
    throw new NoSuchElementException("No exceptions present."
        + " This method should be called after success() method!");
  }
}
