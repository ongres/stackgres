/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation;

public interface DefaultCustomResourceHolder<T> {

  boolean isDefaultCustomResource(T customResource);

  boolean isDefaultCustomResource(String name, String namespace);

  void holdDefaultCustomResource(T customResource);

}
