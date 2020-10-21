/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.operator.conciliation.comparator.StatefulSetComparator;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsStatefulSetComparator;

public class StatefulSetComparatorTest {

  protected final StatefulSetComparator comparator = new DistributedLogsStatefulSetComparator();

  protected StatefulSet required;
  protected StatefulSet deployed;

}
