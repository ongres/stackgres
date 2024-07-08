/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigDeploy;
import io.stackgres.common.crd.sgconfig.StackGresConfigSpec;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class WebConsoleServiceRole
    implements ResourceGenerator<StackGresConfigContext> {

  private final String sgConfigNamespace = OperatorProperty.SGCONFIG_NAMESPACE.get()
      .orElseGet(OperatorProperty.OPERATOR_NAMESPACE::getString);
  private final String operatorNamespace = OperatorProperty.OPERATOR_NAMESPACE.getString();

  private final LabelFactoryForConfig labelFactory;

  @Inject
  public WebConsoleServiceRole(LabelFactoryForConfig labelFactory) {
    this.labelFactory = labelFactory;
  }

  /**
   * Create the Secret for Web Console.
   */
  @Override
  public @NotNull Stream<HasMetadata> generateResource(StackGresConfigContext context) {
    if (sgConfigNamespace.or(() -> operatorNamespace).equals(operatorNamespace)
        && (!Optional.ofNullable(context.getSource().getSpec())
            .map(StackGresConfigSpec::getDeploy)
            .map(StackGresConfigDeploy::getRestapi)
            .orElse(true)
            || (context.getWebConsoleServiceAccount()
                .filter(serviceAccount -> Optional
                    .ofNullable(serviceAccount.getMetadata().getOwnerReferences())
                    .stream()
                    .flatMap(List::stream)
                    .noneMatch(ResourceUtil.getControllerOwnerReference(context.getSource())::equals))
                .isPresent()
                && OperatorProperty.DISABLE_RESTAPI_SERVICE_ACCOUNT_IF_NOT_EXISTS.getBoolean()))) {
      return Stream.of();
    }

    final StackGresConfig config = context.getSource();
    final String namespace = config.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(config);

    return Stream.of(new RoleBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(WebConsoleDeployment.name(config))
        .withLabels(labels)
        .endMetadata()
        .withRules(
            new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Secret.class))
            .withResources(HasMetadata.getPlural(Secret.class))
            .withVerbs("list", "get")
            .build())
        .build(),
        new RoleBindingBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(WebConsoleDeployment.name(config))
        .withLabels(labels)
        .endMetadata()
        .withNewRoleRef()
        .withApiGroup(HasMetadata.getGroup(Role.class))
        .withKind(HasMetadata.getKind(Role.class))
        .withName(WebConsoleDeployment.name(config))
        .endRoleRef()
        .addNewSubject()
        .withKind(HasMetadata.getKind(ServiceAccount.class))
        .withNamespace(namespace)
        .withName(WebConsoleDeployment.name(config))
        .endSubject()
        .build());
  }

}
