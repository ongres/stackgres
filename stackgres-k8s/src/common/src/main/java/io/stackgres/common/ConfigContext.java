/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;

public interface ConfigContext<T extends Enum<?>> {

  Optional<String> getProperty(T configProperty);

  String get(T configProperty);

  boolean getAsBoolean(T configProperty);

}
