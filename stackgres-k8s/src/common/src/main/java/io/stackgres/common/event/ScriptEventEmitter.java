/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.event;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgscript.StackGresScript;

@ApplicationScoped
@EventEmitterType(StackGresScript.class)
public class ScriptEventEmitter extends AbstractEventEmitter<StackGresScript> {

}
