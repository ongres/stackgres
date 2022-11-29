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
import com.google.common.collect.ImmutableList;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.apiweb.dto.script.ScriptEntry;
import io.stackgres.apiweb.dto.script.ScriptFrom;
import io.stackgres.apiweb.dto.script.ScriptSpec;
import io.stackgres.apiweb.dto.script.ScriptStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;

@ApplicationScoped
public class ScriptTransformer
    extends AbstractDependencyResourceTransformer<ScriptDto, StackGresScript> {

  private final ObjectMapper mapper;

  @Inject
  public ScriptTransformer(ObjectMapper mapper) {
    super();
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
    transformation.setStatus(getResourceStatus(clusters));
    return transformation;
  }

  private StackGresScriptSpec getCustomResourceSpec(ScriptSpec source) {
    if (source == null) {
      return null;
    }
    StackGresScriptSpec transformation = new StackGresScriptSpec();
    transformation.setManagedVersions(source.isManagedVersions());
    transformation.setContinueOnError(source.isContinueOnError());
    if (source.getScripts() != null) {
      transformation.setScripts(source.getScripts().stream()
          .map(this::getCustomResourceScriptEntry)
          .collect(ImmutableList.toImmutableList()));
    }
    return transformation;
  }

  public StackGresScriptEntry getCustomResourceScriptEntry(
      ScriptEntry source) {
    if (source == null) {
      return null;
    }
    StackGresScriptEntry transformation =
        new StackGresScriptEntry();
    transformation.setName(source.getName());
    transformation.setId(source.getId());
    transformation.setVersion(source.getVersion());
    transformation.setDatabase(source.getDatabase());
    transformation.setUser(source.getUser());
    transformation.setRetryOnError(source.getRetryOnError());
    transformation.setStoreStatusInDatabase(source.getStoreStatusInDatabase());
    transformation.setWrapInTransaction(source.getWrapInTransaction());
    transformation.setScript(source.getScript());
    transformation.setScriptFrom(getCustomResourceScriptFrom(source.getScriptFrom()));
    return transformation;
  }

  private StackGresScriptFrom getCustomResourceScriptFrom(
      ScriptFrom source) {
    if (source == null) {
      return null;
    }
    StackGresScriptFrom transformation =
        new StackGresScriptFrom();
    transformation.setConfigMapKeyRef(source.getConfigMapKeyRef());
    transformation.setSecretKeyRef(source.getSecretKeyRef());
    return transformation;
  }

  private ScriptSpec getResourceSpec(StackGresScriptSpec source) {
    ScriptSpec transformation = new ScriptSpec();
    transformation.setManagedVersions(source.isManagedVersions());
    transformation.setContinueOnError(source.isContinueOnError());
    if (source.getScripts() != null) {
      transformation.setScripts(source.getScripts().stream()
          .map(this::getResourceScriptEntry)
          .collect(ImmutableList.toImmutableList()));
    }
    return transformation;
  }

  private ScriptEntry getResourceScriptEntry(
      StackGresScriptEntry source) {
    if (source == null) {
      return null;
    }
    ScriptEntry transformation =
        new ScriptEntry();
    transformation.setName(source.getName());
    transformation.setId(source.getId());
    transformation.setVersion(source.getVersion());
    transformation.setDatabase(source.getDatabase());
    transformation.setUser(source.getUser());
    transformation.setRetryOnError(source.getRetryOnError());
    transformation.setStoreStatusInDatabase(source.getStoreStatusInDatabase());
    transformation.setWrapInTransaction(source.getWrapInTransaction());
    transformation.setScript(source.getScript());
    transformation.setScriptFrom(getResourceScriptFrom(source.getScriptFrom()));
    return transformation;
  }

  private ScriptFrom getResourceScriptFrom(
      StackGresScriptFrom source) {
    if (source == null) {
      return null;
    }
    ScriptFrom transformation =
        new ScriptFrom();
    transformation.setConfigMapKeyRef(source.getConfigMapKeyRef());
    transformation.setSecretKeyRef(source.getSecretKeyRef());
    return transformation;
  }

  private ScriptStatus getResourceStatus(List<String> clusters) {
    ScriptStatus transformation = new ScriptStatus();
    transformation.setClusters(clusters);
    return transformation;
  }

}
