/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Service;
import io.stackgres.common.event.AbstractEventEmitter;

@ApplicationScoped
public class OperatorEventEmitter extends AbstractEventEmitter<Service> {

}
