/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator;

import java.io.Closeable;

import org.jooq.lambda.fi.lang.CheckedRunnable;

public interface OperatorRunner extends CheckedRunnable, Closeable {
}
