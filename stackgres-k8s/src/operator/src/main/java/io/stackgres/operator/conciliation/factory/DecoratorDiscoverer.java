/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

public interface DecoratorDiscoverer<T> {

  List<Decorator<T>> discoverDecorator(T context);
}
