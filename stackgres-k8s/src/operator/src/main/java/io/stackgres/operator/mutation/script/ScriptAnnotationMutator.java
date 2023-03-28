/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.script;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.mutation.AbstractAnnotationMutator;

@ApplicationScoped
public class ScriptAnnotationMutator
    extends AbstractAnnotationMutator<StackGresScript, StackGresScriptReview>
    implements ScriptMutator {

  // On version removed change this code to use the oldest one
  private static final long VERSION_1_4 = StackGresVersion.V_1_4.getVersionAsNumber();

  @Override
  public Optional<Map<String, String>> getAnnotationsToOverwrite(StackGresScript resource) {
    final long version = StackGresVersion.getStackGresVersionAsNumber(resource);
    if (VERSION_1_4 > version) {
      return Optional.of(Map.of(StackGresContext.VERSION_KEY, StackGresVersion.V_1_4.getVersion()));
    }
    return Optional.empty();
  }

}
