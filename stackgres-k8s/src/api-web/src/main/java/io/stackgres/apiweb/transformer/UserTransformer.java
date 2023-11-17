/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.stackgres.apiweb.dto.user.UserDto;
import io.stackgres.apiweb.dto.user.UserRoleRef;
import io.stackgres.apiweb.security.TokenUtils;
import io.stackgres.common.StackGresContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserTransformer
    extends AbstractResourceTransformer<UserDto, Secret> {

  private final ObjectMapper mapper;

  @Inject
  public UserTransformer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Secret toCustomResource(UserDto source, Secret original) {
    Secret transformation = Optional.ofNullable(original)
        .map(o -> mapper.convertValue(original, Secret.class))
        .orElseGet(Secret::new);
    transformation.setMetadata(getCustomResourceMetadata(source, original));
    if (transformation.getMetadata().getLabels() == null) {
      transformation.getMetadata().setLabels(new HashMap<>());
    } else {
      transformation.getMetadata().setLabels(new HashMap<>(
          transformation.getMetadata().getLabels()));
    }
    transformation.getMetadata().getLabels().putAll(
        Map.of(StackGresContext.AUTH_KEY, StackGresContext.AUTH_USER_VALUE));
    Map<String, String> data = new HashMap<>();
    Optional.ofNullable(source.getK8sUsername())
        .ifPresent(k8sUsername -> data.put(StackGresContext.REST_K8SUSER_KEY, k8sUsername));
    Optional.ofNullable(source.getApiUsername())
        .ifPresent(apiUsername -> data.put(StackGresContext.REST_APIUSER_KEY, apiUsername));
    Optional.ofNullable(source.getPassword())
        .filter(password -> Objects.nonNull(source.getK8sUsername()))
        .ifPresent(password -> data.put(StackGresContext.REST_PASSWORD_KEY,
            TokenUtils.sha256(
                Optional.ofNullable(source.getApiUsername())
                .orElse(source.getK8sUsername())
                + password)));
    transformation.setData(ResourceUtil.encodeSecret(data));
    return transformation;
  }

  @Override
  public UserDto toDto(Secret source) {
    throw new UnsupportedOperationException();
  }

  public UserDto toDto(
      Secret source,
      List<RoleBinding> roleBindings,
      List<ClusterRoleBinding> clusterRoleBindings) {
    UserDto transformation = new UserDto();
    transformation.setMetadata(getDtoMetadata(source));
    Map<String, String> data = ResourceUtil.decodeSecret(source.getData());
    transformation.setK8sUsername(
        Optional.ofNullable(data.get(StackGresContext.REST_K8SUSER_KEY))
        .orElse(null));
    transformation.setApiUsername(
        Optional.ofNullable(data.get(StackGresContext.REST_APIUSER_KEY))
        .orElse(null));
    addRoles(transformation, roleBindings);
    addClusterRoles(transformation, clusterRoleBindings);
    return transformation;
  }

  private void addRoles(UserDto user, List<RoleBinding> roleBindings) {
    Subject userSubject = user.getSubject();
    if (user.getRoles() == null) {
      user.setRoles(new ArrayList<>());
    } else {
      user.setRoles(new ArrayList<>(
          user.getRoles()));
    }
    roleBindings.stream()
        .filter(roleBinding -> Optional.ofNullable(roleBinding.getSubjects())
            .stream()
            .flatMap(List::stream)
            .anyMatch(userSubject::equals))
        .forEach(roleBinding -> user.getRoles().add(
            new UserRoleRef(
                user.getMetadata().getNamespace(),
                roleBinding.getRoleRef().getName())));
  }

  private void addClusterRoles(UserDto user, List<ClusterRoleBinding> clusterRoleBindings) {
    Subject userSubject = user.getSubject();
    if (user.getClusterRoles() == null) {
      user.setClusterRoles(new ArrayList<>());
    } else {
      user.setClusterRoles(new ArrayList<>(
          user.getClusterRoles()));
    }
    clusterRoleBindings.stream()
        .filter(clusterRoleBinding -> Optional.ofNullable(clusterRoleBinding.getSubjects())
            .stream()
            .flatMap(List::stream)
            .anyMatch(userSubject::equals))
        .forEach(clusterRoleBinding -> user.getClusterRoles().add(
            new UserRoleRef(
                clusterRoleBinding.getRoleRef().getName())));
  }

}
