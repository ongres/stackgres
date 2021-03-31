/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import io.smallrye.mutiny.Uni;

public interface Watcher<T> {

  Uni<T> waitUntilIsReady(String name, String namespace);

  Uni<Void> waitUntilIsRemoved(String name, String namespace);

  default Uni<T> waitUntilIsReplaced(T pod){
    throw new UnsupportedOperationException("Not supported");
  }

}
