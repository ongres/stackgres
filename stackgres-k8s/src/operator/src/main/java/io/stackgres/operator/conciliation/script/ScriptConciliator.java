/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.Conciliator;

@ApplicationScoped
public class ScriptConciliator extends Conciliator<StackGresScript> {

}
