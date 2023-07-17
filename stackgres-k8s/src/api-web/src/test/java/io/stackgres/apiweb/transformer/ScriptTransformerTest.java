/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.dto.script.ScriptStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ScriptTransformerTest {

  @Inject
  ScriptTransformer transformer;

  public static TransformerTuple<ScriptDto, StackGresScript> createScript() {

    StackGresScript source = new StackGresScript();
    ScriptDto target = new ScriptDto();

    var metadata = TransformerTestUtil.createMetadataTuple();
    source.setMetadata(metadata.source());
    target.setMetadata(metadata.target());

    var spec = createSpec();
    source.setSpec(spec.source());
    target.setSpec(spec.target());

    var status = createStatus();
    source.setStatus(status.source());

    target.setStatus(new ScriptStatus());
    target.getStatus().setClusters(List.of(StringUtils.getRandomClusterName()));

    return new TransformerTuple<>(target, source);
  }

  private static TransformerTuple<StackGresScriptStatus, StackGresScriptStatus> createStatus() {
    var statusTuple = TransformerTestUtil
        .fillTupleWithRandomData(StackGresScriptStatus.class, StackGresScriptStatus.class);

    return statusTuple;
  }

  private static TransformerTuple<ScriptSpec, StackGresScriptSpec> createSpec() {
    var specTuple = TransformerTestUtil
        .fillTupleWithRandomData(ScriptSpec.class, StackGresScriptSpec.class);

    return specTuple;
  }

  @Test
  void testScriptTransformation() {

    var tuple = createScript();

    final List<String> clusters = Optional.of(tuple.target())
        .map(ScriptDto::getStatus)
        .map(ScriptStatus::getClusters).orElse(List.of());

    TransformerTestUtil.assertTransformation(transformer, tuple, clusters);
  }
}
