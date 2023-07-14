/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.controller;

import io.fabric8.kubernetes.api.model.Service;
import io.stackgres.common.event.AbstractEventEmitter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OperatorEventEmitter extends AbstractEventEmitter<Service> {

}
