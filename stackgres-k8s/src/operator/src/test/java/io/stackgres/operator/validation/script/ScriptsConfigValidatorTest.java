/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.script;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.ErrorType;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgscript.StackGresScriptFrom;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.common.StackGresScriptReview;
import io.stackgres.operator.utils.ValidationUtils;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScriptsConfigValidatorTest {

  private ScriptsConfigValidator validator;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @Mock
  private ResourceFinder<ConfigMap> configMapFinder;

  @BeforeEach
  void setUp() {
    validator = new ScriptsConfigValidator(secretFinder, configMapFinder);
  }

  @Test
  void givenAValidCreation_shouldPass() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");

    validator.validate(review);
  }

  @Test
  void givenAnUpdateWithDecreasedLastId_shouldFail()
      throws ValidationFailed {
    final StackGresScriptReview review = getUpdateReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getStatus().setLastId(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Status lastId must not decrease");
  }

  @Test
  void givenACreationWithLastIdLowerThanSomeIds_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getStatus().setLastId(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries ids must be less or equals than status lastId");
  }

  @Test
  void givenACreationWithDuplicatedId_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getSpec().getScripts().get(0).setId(1);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain unique ids");
  }

  @Test
  void givenACreationWithUnrelatedStatusId_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getStatus().getScripts().remove(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script entries must contain a matching id"
            + " for each script status entry");
  }

  @Test
  void givenACreationWithUnrelatedId_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    prepareForScript(review, "test", "CREATE DATABASE test;");
    review.getRequest().getObject().getSpec().getScripts().remove(0);

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.CONSTRAINT_VIOLATION,
        "Script status entries must contain a matching id"
            + " for each script entry");
  }

  @Test
  void givenAnUpdateUsingScriptFromSecret_shouldValidateSecretKeyReference()
      throws ValidationFailed {
    final StackGresScriptReview review = getUpdateReview();

    String randomSecretKey = StringUtil.generateRandom();
    String randomSecretName = StringUtil.generateRandom();

    final StackGresScript script = review.getRequest().getObject();
    prepareForSecret(review, randomSecretName, randomSecretKey);

    final String namespace = script.getMetadata().getNamespace();
    when(secretFinder.findByNameAndNamespace(randomSecretName, namespace))
        .thenReturn(Optional.of(new SecretBuilder()
            .withNewMetadata()
            .withName(randomSecretName)
            .withNamespace(namespace)
            .endMetadata()
            .withData(ImmutableMap.of(randomSecretKey, "CREATE DATABASE test;"))
            .build()));

    validator.validate(review);

    final List<StackGresScriptEntry> scripts = script.getSpec().getScripts();
    verify(secretFinder, times(scripts.size()))
        .findByNameAndNamespace(randomSecretName, namespace);
  }

  @Test
  void givenACreationUsingScriptFromSecret_shouldValidateSecretKeyReference()
      throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    String randomSecretKey = StringUtil.generateRandom();
    String randomSecretName = StringUtil.generateRandom();

    final StackGresScript script = review.getRequest().getObject();
    prepareForSecret(review, randomSecretName, randomSecretKey);

    final String namespace = script.getMetadata().getNamespace();
    when(secretFinder.findByNameAndNamespace(randomSecretName, namespace))
        .thenReturn(Optional.of(new SecretBuilder()
            .withNewMetadata()
            .withName(randomSecretName)
            .withNamespace(namespace)
            .endMetadata()
            .withData(ImmutableMap.of(randomSecretKey, "CREATE DATABASE test;"))
            .build()));

    validator.validate(review);

    final List<StackGresScriptEntry> scripts = script.getSpec().getScripts();
    verify(secretFinder, times(scripts.size()))
        .findByNameAndNamespace(randomSecretName, namespace);
  }

  @Test
  void givenACreationUsingScriptFromNonexistentSecret_shouldFail() {
    final StackGresScriptReview review = getCreationReview();

    String randomSecretKey = StringUtil.generateRandom();
    String randomSecretName = StringUtil.generateRandom();

    final StackGresScript script = review.getRequest().getObject();
    prepareForSecret(review, randomSecretName, randomSecretKey);

    final String namespace = script.getMetadata().getNamespace();
    when(secretFinder.findByNameAndNamespace(randomSecretName, namespace))
        .thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Referenced Secret " + randomSecretName + " does not exists in namespace " + namespace);

    verify(secretFinder).findByNameAndNamespace(randomSecretName, namespace);
  }

  @Test
  void givenACreationUsingScriptFromNonexistentSecretKey_shouldFail() {
    final StackGresScriptReview review = getCreationReview();

    String randomSecretKey = StringUtil.generateRandom();
    String randomSecretName = StringUtil.generateRandom();

    final StackGresScript script = review.getRequest().getObject();
    prepareForSecret(review, randomSecretName, randomSecretKey);

    final String namespace = script.getMetadata().getNamespace();
    when(secretFinder.findByNameAndNamespace(randomSecretName, namespace))
        .thenReturn(Optional.of(new SecretBuilder()
            .withNewMetadata()
            .withName(randomSecretName)
            .withNamespace(namespace)
            .endMetadata()
            .withData(ImmutableMap.of())
            .build()));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Key " + randomSecretKey + " does not exists in Secret " + randomSecretName);

    verify(secretFinder).findByNameAndNamespace(randomSecretName, namespace);
  }

  @Test
  void givenACreationUsingScriptsFromPlainConfigMap_shouldValidateConfigMapReference()
      throws ValidationFailed {

    final StackGresScriptReview review = getCreationReview();

    String randomConfigMapName = StringUtil.generateRandom();
    String randomConfigMapKey = StringUtil.generateRandom();

    prepareForConfigMap(review, randomConfigMapName, randomConfigMapKey);

    final StackGresScript script = review.getRequest().getObject();

    final String namespace = script.getMetadata().getNamespace();
    when(configMapFinder.findByNameAndNamespace(randomConfigMapName, namespace))
        .thenReturn(Optional.of(new ConfigMapBuilder()
            .withNewMetadata()
            .withName(randomConfigMapName)
            .withNamespace(namespace)
            .endMetadata()
            .withData(ImmutableMap.of(randomConfigMapKey, "CREATE DATABASE test;"))
            .build()));

    validator.validate(review);

    final List<StackGresScriptEntry> scripts = script.getSpec().getScripts();
    verify(configMapFinder, times(scripts.size()))
        .findByNameAndNamespace(randomConfigMapName, namespace);
  }

  @Test
  void givenACreationUsingScriptsFromBinaryConfigMap_shouldValidateConfigMapReference()
      throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    String randomConfigMapName = StringUtil.generateRandom();
    String randomConfigMapKey = StringUtil.generateRandom();

    prepareForConfigMap(review, randomConfigMapName, randomConfigMapKey);

    final StackGresScript script = review.getRequest().getObject();

    final String namespace = script.getMetadata().getNamespace();
    when(configMapFinder.findByNameAndNamespace(randomConfigMapName, namespace))
        .thenReturn(Optional.of(new ConfigMapBuilder()
            .withNewMetadata()
            .withName(randomConfigMapName)
            .withNamespace(namespace)
            .endMetadata()
            .withBinaryData(ImmutableMap.of(randomConfigMapKey, "CREATE DATABASE test;"))
            .build()));

    validator.validate(review);

    final List<StackGresScriptEntry> scripts = script.getSpec().getScripts();
    verify(configMapFinder, times(scripts.size()))
        .findByNameAndNamespace(randomConfigMapName, namespace);
  }

  @Test
  void givenACreationUsingScriptsFromNonexistentConfigMap_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    String randomConfigMapName = StringUtil.generateRandom();
    String randomConfigMapKey = StringUtil.generateRandom();

    prepareForConfigMap(review, randomConfigMapName, randomConfigMapKey);

    final StackGresScript script = review.getRequest().getObject();

    final String namespace = script.getMetadata().getNamespace();
    when(configMapFinder.findByNameAndNamespace(randomConfigMapName, namespace))
        .thenReturn(Optional.empty());

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Referenced ConfigMap " + randomConfigMapName + " does not exists in namespace "
            + namespace);

    verify(configMapFinder)
        .findByNameAndNamespace(randomConfigMapName, namespace);
  }

  @Test
  void givenACreationUsingScriptsFromNonexistentConfigMapKey_shouldFail() throws ValidationFailed {
    final StackGresScriptReview review = getCreationReview();

    String randomConfigMapName = StringUtil.generateRandom();
    String randomConfigMapKey = StringUtil.generateRandom();

    prepareForConfigMap(review, randomConfigMapName, randomConfigMapKey);

    final StackGresScript script = review.getRequest().getObject();

    final String namespace = script.getMetadata().getNamespace();
    when(configMapFinder.findByNameAndNamespace(randomConfigMapName, namespace))
        .thenReturn(Optional.of(new ConfigMapBuilder()
            .withNewMetadata()
            .withName(randomConfigMapName)
            .withNamespace(namespace)
            .endMetadata()
            .withBinaryData(ImmutableMap.of())
            .withData(ImmutableMap.of())
            .build()));

    ValidationUtils.assertValidationFailed(() -> validator.validate(review),
        ErrorType.INVALID_CR_REFERENCE,
        "Key " + randomConfigMapKey + " does not exists in ConfigMap " + randomConfigMapName);

    verify(configMapFinder)
        .findByNameAndNamespace(randomConfigMapName, namespace);
  }

  private StackGresScriptReview getCreationReview() {
    return JsonUtil
        .readFromJson("script_allow_requests/valid_creation.json",
            StackGresScriptReview.class);
  }

  private StackGresScriptReview getUpdateReview() {
    return JsonUtil
        .readFromJson("script_allow_requests/scripts_config_update.json",
            StackGresScriptReview.class);
  }

  private void prepareForScript(StackGresScriptReview review, String name, String script) {
    review.getRequest().getObject().getSpec().getScripts().forEach(s -> {
      s.setScriptFrom(null);
      s.setName(name);
      s.setScript(script);
    });
  }

  private void prepareForSecret(StackGresScriptReview review, String name, String key) {
    review.getRequest().getObject().getSpec().getScripts().forEach(s -> {
      s.setName(null);
      s.setScript(null);
      s.setDatabase(null);
      s.setScriptFrom(new StackGresScriptFrom());
      s.getScriptFrom().setSecretKeyRef(new SecretKeySelector(key, name));
    });
  }

  private void prepareForConfigMap(StackGresScriptReview review, String name, String key) {
    review.getRequest().getObject().getSpec().getScripts().forEach(s -> {
      s.setName(null);
      s.setScript(null);
      s.setDatabase(null);
      s.setScriptFrom(new StackGresScriptFrom());
      s.getScriptFrom().setConfigMapKeyRef(new ConfigMapKeySelector(key, name));
    });
  }

}
