/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class ScriptAnnotationMutator
    extends AbstractAnnotationMutator<StackGresScript, StackGresScriptReview>
    implements ScriptMutator {
}
