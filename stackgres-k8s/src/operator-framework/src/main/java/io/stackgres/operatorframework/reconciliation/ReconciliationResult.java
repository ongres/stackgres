/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.jooq.lambda.Seq;

public class ReconciliationResult<R> {
  private final R result;
  private final List<Exception> exceptions;

  @SuppressWarnings("null")
  public ReconciliationResult() {
    this((R) null);
  }

  public ReconciliationResult(R result) {
    this(result, List.of());
  }

  public ReconciliationResult(Exception exception) {
    this(null, exception);
  }

  public ReconciliationResult(R result,
      Exception exception) {
    this(result, List.of(exception));
  }

  public ReconciliationResult(
      List<Exception> exceptions) {
    this(null, exceptions);
  }

  public ReconciliationResult(R result,
      List<Exception> exceptions) {
    this.result = result;
    this.exceptions = exceptions;
  }

  public Optional<R> result() {
    return Optional.ofNullable(result);
  }

  public List<Exception> getExceptions() {
    return exceptions;
  }

  public boolean success() {
    return exceptions.isEmpty();
  }

  public ReconciliationResult<Void> join(ReconciliationResult<?> other) {
    return new ReconciliationResult<Void>(null,
        Seq.seq(exceptions)
        .append(other.exceptions)
        .toList());
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
