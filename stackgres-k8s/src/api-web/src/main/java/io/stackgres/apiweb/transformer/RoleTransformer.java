/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.stackgres.apiweb.dto.role.RoleDto;
import io.stackgres.common.StackGresContext;

@ApplicationScoped
public class RoleTransformer
    extends AbstractResourceTransformer<RoleDto, Role> {

  private final ObjectMapper mapper;

  @Inject
  public RoleTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Role toCustomResource(RoleDto source, Role original) {
    Role transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, Role.class))
        .orElseGet(Role::new);
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
  public RoleDto toDto(Role source) {
    RoleDto transformation = new RoleDto();
    transformation.setMetadata(getDtoMetadata(source));
    transformation.setRules(source.getRules());
    return transformation;
  }

}
