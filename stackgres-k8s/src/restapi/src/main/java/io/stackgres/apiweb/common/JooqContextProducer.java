/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.common;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@Dependent
public class JooqContextProducer {

  @Inject Instance<AgroalDataSource> dataSource;

  @Produces
  public DSLContext dslContext() {
    return DSL.using(dataSource.get(), SQLDialect.POSTGRES);
  }

}
