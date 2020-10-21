/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.List;

public interface ResourceGenerationDiscoverer<T> {

  List<ResourceGenerator<T>> getResourceGenerators(T context);
}
