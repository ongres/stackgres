/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

public interface Transformer<T, S> {

  S toSource(T target);

  T toTarget(S source);

}
