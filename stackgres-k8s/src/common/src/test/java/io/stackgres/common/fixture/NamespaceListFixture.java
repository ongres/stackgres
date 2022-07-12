/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.NamespaceListBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class NamespaceListFixture extends Fixture<NamespaceList> {

  public NamespaceListFixture loadDefault() {
    fixture = readFromJson(NAMESPACE_LIST_JSON);
    return this;
  }

  public NamespaceListBuilder getBuilder() {
    return new NamespaceListBuilder(fixture);
  }

}
