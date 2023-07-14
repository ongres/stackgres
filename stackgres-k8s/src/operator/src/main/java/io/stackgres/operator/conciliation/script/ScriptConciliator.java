/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.conciliation.Conciliator;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ScriptConciliator extends Conciliator<StackGresScript> {

}
