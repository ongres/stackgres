/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common.fixture;

import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.common.StackGresScriptReviewBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class ScriptReviewFixture extends Fixture<StackGresScriptReview> {

  public static ScriptReviewFixture fixture() {
    return new ScriptReviewFixture();
  }

  public ScriptReviewFixture loadCreate() {
    fixture = readFromJson(STACKGRES_SCRIPT_ADMISSION_REVIEW_CREATE_JSON);
    return this;
  }

  public ScriptReviewFixture loadUpdate() {
    fixture = readFromJson(STACKGRES_SCRIPT_ADMISSION_REVIEW_UPDATE_JSON);
    return this;
  }

  public ScriptReviewFixture loadDelete() {
    fixture = readFromJson(STACKGRES_SCRIPT_ADMISSION_REVIEW_DELETE_JSON);
    return this;
  }

  public ScriptReviewFixture loadScriptsConfigUpdate() {
    fixture = readFromJson(STACKGRES_SCRIPT_ADMISSION_REVIEW_SCRIPTS_CONFIG_UPDATE_JSON);
    return this;
  }

  public StackGresScriptReviewBuilder getBuilder() {
    return new StackGresScriptReviewBuilder(fixture);
  }

}
