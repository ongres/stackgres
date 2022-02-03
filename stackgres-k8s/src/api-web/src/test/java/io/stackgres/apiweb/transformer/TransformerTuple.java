/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

public class TransformerTuple<T, S> {
  private final T target;
  private final S source;

  public TransformerTuple(T target, S source) {
    this.target = target;
    this.source = source;
  }

  public T getTarget() {
    return target;
  }

  public S getSource() {
    return source;
  }
}
