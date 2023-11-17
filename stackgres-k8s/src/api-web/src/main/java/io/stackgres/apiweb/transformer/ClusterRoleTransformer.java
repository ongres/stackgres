/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.stackgres.apiweb.dto.clusterrole.ClusterRoleDto;
import io.stackgres.common.StackGresContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterRoleTransformer
    extends AbstractResourceTransformer<ClusterRoleDto, ClusterRole> {

  private final ObjectMapper mapper;

  @Inject
  public ClusterRoleTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public ClusterRole toCustomResource(ClusterRoleDto source, ClusterRole original) {
    ClusterRole transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, ClusterRole.class))
        .orElseGet(ClusterRole::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    if (transformation.getMetadata().getLabels() == null) {
      transformation.getMetadata().setLabels(new HashMap<>());
    } else {
      transformation.getMetadata().setLabels(new HashMap<>(
          transformation.getMetadata().getLabels()));
    }
    transformation.getMetadata().getLabels().putAll(
        Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE));
    transformation.setRules(source.getRules());
    return transformation;
  }

  @Override
  public ClusterRoleDto toDto(ClusterRole source) {
    ClusterRoleDto transformation = new ClusterRoleDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setRules(source.getRules());
    return transformation;
  }

}
