/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.mock;

import io.quarkus.test.Mock;
import jakarta.inject.Singleton;

@Mock
@Singleton
public class ExtensionMetadataManagerMock
    extends io.stackgres.common.extension.ExtensionMetadataManagerMock {

}
