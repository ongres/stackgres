/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.testutil.fixture;

import io.stackgres.fixture.processor.PathsAsConstants;

@PathsAsConstants(value = "src/main/resources", regExp = "^.*\\.(json|json\\.xz)$")
public interface JsonFixture extends JsonFixtureWithPaths {

}
