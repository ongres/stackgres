/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.dto.script.ScriptStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.crd.sgscript.StackGresScriptStatus;

@ApplicationScoped
public class ScriptTransformer
    extends AbstractDependencyResourceTransformer<ScriptDto, StackGresScript> {

  private final ObjectMapper mapper;

  @Inject
  public ScriptTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public StackGresScript toCustomResource(ScriptDto source,
      StackGresScript original) {
    StackGresScript transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, StackGresScript.class))
        .orElseGet(StackGresScript::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    transformation.setSpec(getCustomResourceSpec(source.getSpec()));
    return transformation;
  }

  @Override
  public ScriptDto toResource(StackGresScript source, List<String> clusters) {
    ScriptDto transformation = new ScriptDto();
    transformation.setMetadata(getResourceMetadata(source));
    transformation.setSpec(getResourceSpec(source.getSpec()));
    transformation.setStatus(getResourceStatus(source.getStatus()));
    if (transformation.getStatus() == null) {
      transformation.setStatus(new ScriptStatus());
    }
    transformation.getStatus().setClusters(clusters);
    return transformation;
  }

  private StackGresScriptSpec getCustomResourceSpec(ScriptSpec source) {
    return mapper.convertValue(source, StackGresScriptSpec.class);
  }

  private ScriptSpec getResourceSpec(StackGresScriptSpec source) {
    return mapper.convertValue(source, ScriptSpec.class);
  }

  private ScriptStatus getResourceStatus(StackGresScriptStatus source) {
    return mapper.convertValue(source, ScriptStatus.class);
  }

  public StackGresScriptEntry getCustomResourceScriptEntry(ScriptEntry source) {
    return mapper.convertValue(source, StackGresScriptEntry.class);
  }
}
