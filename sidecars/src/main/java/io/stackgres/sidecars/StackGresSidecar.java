/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.sidecars;

import io.stackgres.common.StackGresSidecarTransformer;
import io.stackgres.sidecars.pgbouncer.PgBouncer;
import io.stackgres.sidecars.pgexporter.PostgresExporter;
import io.stackgres.sidecars.pgutils.PostgresUtil;

public enum StackGresSidecar {

  POSTGRES_UTIL("postgres-util", new PostgresUtil()),
  PGBOUNCER("connection-pooling", new PgBouncer()),
  PROMETHEUS_POSTGRES_EXPORTER("prometheus-postgres-exporter", new PostgresExporter());

  private final String name;
  private final StackGresSidecarTransformer<?> sidecar;

  private StackGresSidecar(String name, StackGresSidecarTransformer<?> sidecar) {
    this.name = name;
    this.sidecar = sidecar;
  }

  public String getName() {
    return name;
  }

  public StackGresSidecarTransformer<?> getSidecar() {
    return sidecar;
  }

  /**
   * Get a {@code StackGresSidecar} from name.
   */
  public static StackGresSidecar fromName(String name) {
    for (StackGresSidecar sidecar : StackGresSidecar.values()) {
      if (sidecar.getName().equals(name)) {
        return sidecar;
      }
    }
    throw new IllegalStateException("Unknown sidecar with name " + name);
  }
}
