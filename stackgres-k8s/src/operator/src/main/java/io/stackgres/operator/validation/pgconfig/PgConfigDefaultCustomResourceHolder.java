/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pgconfig;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.validation.AbstractDefaultCustomResourceHolder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PgConfigDefaultCustomResourceHolder
    extends AbstractDefaultCustomResourceHolder<StackGresPostgresConfig> {

}
