/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.configuration;

import io.stackgres.common.StackGresPropertyContext;
import io.stackgres.stream.app.StreamProperty;
import jakarta.inject.Singleton;

@Singleton
public class StreamPropertyContext
    implements StackGresPropertyContext<StreamProperty> {

}
