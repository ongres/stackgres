/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import javax.inject.Singleton;

import io.stackgres.operator.conciliation.OperatorVersionBinder;

@Singleton
@OperatorVersionBinder
public class DistributedLogsAnnotationDecorator
    extends AbstractDistributedLogsAnnotationDecorator {

}
