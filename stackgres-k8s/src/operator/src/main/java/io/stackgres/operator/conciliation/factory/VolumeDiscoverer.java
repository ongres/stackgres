/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.Map;

public interface VolumeDiscoverer<T> {

  Map<String, VolumePair> discoverVolumes(T context);

}
