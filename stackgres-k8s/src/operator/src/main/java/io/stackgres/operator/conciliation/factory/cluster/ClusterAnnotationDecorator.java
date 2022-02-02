/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import javax.inject.Singleton;

import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V12)
public class ClusterAnnotationDecorator extends AbstractClusterAnnotationDecorator {


}
