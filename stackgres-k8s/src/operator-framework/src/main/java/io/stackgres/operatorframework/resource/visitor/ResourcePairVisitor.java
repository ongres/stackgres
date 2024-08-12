/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.resource.visitor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EnvFromSource;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarSource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import io.fabric8.kubernetes.api.model.NodeSelector;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirement;
import io.fabric8.kubernetes.api.model.NodeSelectorTerm;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimSpec;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeSpec;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodAffinity;
import io.fabric8.kubernetes.api.model.PodAffinityTerm;
import io.fabric8.kubernetes.api.model.PodAntiAffinity;
import io.fabric8.kubernetes.api.model.PodDNSConfig;
import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.PreferredSchedulingTerm;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceFieldSelector;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeDevice;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeNodeAffinity;
import io.fabric8.kubernetes.api.model.VolumeResourceRequirements;
import io.fabric8.kubernetes.api.model.WeightedPodAffinityTerm;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.api.model.batch.v1.JobTemplateSpec;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.operatorframework.resource.ResourceUtil;

public class ResourcePairVisitor<T, C> {

  private final C context;

  public ResourcePairVisitor(C context) {
    super();
    this.context = context;
  }

  public C getContext() {
    return context;
  }

  /**
   * Compare resources skipping non-static fields and considering null as default if any is defined.
   */
  public static boolean equals(HasMetadata left, HasMetadata right) {
    return equals(new ResourcePairVisitor<Boolean, Void>(null), left, right);
  }

  /**
   * Compare resources skipping non-static fields and considering null as default if any is defined.
   */
  public static <C> boolean equals(C context, HasMetadata left, HasMetadata right) {
    return equals(new ResourcePairVisitor<Boolean, C>(context), left, right);
  }

  /**
   * Compare resources skipping non-static fields and considering null as default if any is defined
   * using the specified visitor.
   */
  public static <C> boolean equals(ResourcePairVisitor<Boolean, C> resourceVisitor,
      HasMetadata left, HasMetadata right) {
    return resourceVisitor.visit(new PairComparator<>(left, right)).result();
  }

  /**
   * Update left resource with right resource skipping non-static fields and considering null as
   * default if defined.
   */
  public static HasMetadata update(HasMetadata toUpdate, HasMetadata withUpdates) {
    return update(new ResourcePairVisitor<HasMetadata, Void>(null), toUpdate, withUpdates);
  }

  /**
   * Update left resource with right resource skipping non-static fields and considering null as
   * default if defined.
   */
  public static <C> HasMetadata update(C context, HasMetadata toUpdate, HasMetadata withUpdates) {
    return update(new ResourcePairVisitor<HasMetadata, C>(context), toUpdate, withUpdates);
  }

  /**
   * Update left resource with right resource skipping non-static fields and considering null as
   * default if defined using the specified visitor.
   */
  public static <C> HasMetadata update(ResourcePairVisitor<HasMetadata, C> resourceVisitor,
      HasMetadata toUpdate, HasMetadata withUpdates) {
    return resourceVisitor.visit(new PairUpdater<>(toUpdate, withUpdates)).result();
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<HasMetadata, T> visit(PairVisitor<HasMetadata, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(HasMetadata::getApiVersion, HasMetadata::setApiVersion)
        .visit(HasMetadata::getKind)
        .visitWith(HasMetadata::getMetadata, HasMetadata::setMetadata, this::visitMetadata)
        .lastVisitIfBothInstanceOf(StatefulSet.class, this::visitStatefulSet)
        .lastVisitIfBothInstanceOf(Service.class, this::visitService)
        .lastVisitIfBothInstanceOf(ServiceAccount.class, this::visitServiceAccount)
        .lastVisitIfBothInstanceOf(Role.class, this::visitRole)
        .lastVisitIfBothInstanceOf(RoleBinding.class, this::visitRoleBinding)
        .lastVisitIfBothInstanceOf(Secret.class, this::visitSecret)
        .lastVisitIfBothInstanceOf(ConfigMap.class, this::visitConfigMap)
        .lastVisitIfBothInstanceOf(Endpoints.class, this::visitEndpoints)
        .lastVisitIfBothInstanceOf(CronJob.class, this::visitCronJob)
        .lastVisitIfBothInstanceOf(Job.class, this::visitJob)
        .lastVisitIfBothInstanceOf(Pod.class, this::visitPod)
        .lastVisitIfBothInstanceOf(PersistentVolumeClaim.class, this::visitPersistentVolumeClaim)
        .lastVisit(this::visitUnknown);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<HasMetadata, T> visitUnknown(PairVisitor<HasMetadata, T> pairVisitor) {
    throw new IllegalStateException(
        "Can not compare resources of type " + pairVisitor.left.getKind());
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<StatefulSet, T> visitStatefulSet(PairVisitor<StatefulSet, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(StatefulSet::getSpec, StatefulSet::setSpec, this::visitStatefulSetSpec)
        .visitMap(StatefulSet::getAdditionalProperties,
            additionalPropertiesSetter(
                StatefulSet::getAdditionalProperties,
                StatefulSet::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<CronJob, T> visitCronJob(PairVisitor<CronJob, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(CronJob::getSpec, CronJob::setSpec, this::visitCronJobSpec)
        .visitMap(CronJob::getAdditionalProperties,
            additionalPropertiesSetter(
                CronJob::getAdditionalProperties,
                CronJob::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<CronJobSpec, T> visitCronJobSpec(PairVisitor<CronJobSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(CronJobSpec::getConcurrencyPolicy, CronJobSpec::setConcurrencyPolicy)
        .visit(CronJobSpec::getFailedJobsHistoryLimit, CronJobSpec::setFailedJobsHistoryLimit)
        .visit(CronJobSpec::getSchedule, CronJobSpec::setSchedule)
        .visit(CronJobSpec::getStartingDeadlineSeconds, CronJobSpec::setStartingDeadlineSeconds)
        .visit(CronJobSpec::getSuccessfulJobsHistoryLimit,
            CronJobSpec::setSuccessfulJobsHistoryLimit, 3)
        .visit(CronJobSpec::getSuspend, CronJobSpec::setSuspend, false)
        .visitWith(CronJobSpec::getJobTemplate, CronJobSpec::setJobTemplate,
            this::visitJobTemplateSpec)
        .visitMap(CronJobSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                CronJobSpec::getAdditionalProperties,
                CronJobSpec::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Job, T> visitJob(PairVisitor<Job, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(Job::getSpec, Job::setSpec, this::visitJobSpec)
        .visitMap(Job::getAdditionalProperties,
            additionalPropertiesSetter(
                Job::getAdditionalProperties,
                Job::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<JobTemplateSpec, T> visitJobTemplateSpec(
      PairVisitor<JobTemplateSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(JobTemplateSpec::getMetadata, JobTemplateSpec::setMetadata, this::visitMetadata)
        .visitWith(JobTemplateSpec::getSpec, JobTemplateSpec::setSpec,
            this::visitJobSpecFromJobTemplateSpec)
        .visitMap(JobTemplateSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                JobTemplateSpec::getAdditionalProperties,
                JobTemplateSpec::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<JobSpec, T> visitJobSpecFromJobTemplateSpec(
      PairVisitor<JobSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(JobSpec::getActiveDeadlineSeconds, JobSpec::setActiveDeadlineSeconds)
        .visit(JobSpec::getBackoffLimit, JobSpec::setBackoffLimit)
        .visit(JobSpec::getCompletions, JobSpec::setCompletions)
        .visit(JobSpec::getManualSelector, JobSpec::setManualSelector)
        .visit(JobSpec::getParallelism, JobSpec::setParallelism)
        .visit(JobSpec::getTtlSecondsAfterFinished, JobSpec::setTtlSecondsAfterFinished)
        .visitWith(JobSpec::getTemplate, JobSpec::setTemplate, this::visitPodTemplateSpec)
        .visitMap(JobSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                JobSpec::getAdditionalProperties,
                JobSpec::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<JobSpec, T> visitJobSpec(PairVisitor<JobSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(JobSpec::getActiveDeadlineSeconds, JobSpec::setActiveDeadlineSeconds)
        .visit(JobSpec::getBackoffLimit, JobSpec::setBackoffLimit)
        .visit(JobSpec::getCompletions, JobSpec::setCompletions)
        .visit(JobSpec::getManualSelector, JobSpec::setManualSelector)
        .visit(JobSpec::getParallelism, JobSpec::setParallelism)
        .visit(JobSpec::getTtlSecondsAfterFinished, JobSpec::setTtlSecondsAfterFinished, 300)
        .visitWith(JobSpec::getTemplate, JobSpec::setTemplate,
            this::visitPodTemplateSpecFromJobSpec)
        .visitMap(JobSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                JobSpec::getAdditionalProperties,
                JobSpec::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PersistentVolume, T> visitPersistentVolume(
      PairVisitor<PersistentVolume, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(PersistentVolume::getSpec, PersistentVolume::setSpec,
            this::visitPersistentVolumeSpec)
        .visitMap(PersistentVolume::getAdditionalProperties,
            additionalPropertiesSetter(
                PersistentVolume::getAdditionalProperties,
                PersistentVolume::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PersistentVolumeSpec, T> visitPersistentVolumeSpec(
      PairVisitor<PersistentVolumeSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visitList(PersistentVolumeSpec::getAccessModes, PersistentVolumeSpec::setAccessModes)
        .visitList(PersistentVolumeSpec::getMountOptions, PersistentVolumeSpec::setMountOptions)
        .visitWith(PersistentVolumeSpec::getNodeAffinity, PersistentVolumeSpec::setNodeAffinity,
            this::visitVolumeNodeAffinity)
        .visit(PersistentVolumeSpec::getPersistentVolumeReclaimPolicy,
            PersistentVolumeSpec::setPersistentVolumeReclaimPolicy)
        .visit(PersistentVolumeSpec::getStorageClassName, PersistentVolumeSpec::setStorageClassName)
        .visit(PersistentVolumeSpec::getVolumeMode, PersistentVolumeSpec::setVolumeMode)
        .visit(PersistentVolumeSpec::getAwsElasticBlockStore,
            PersistentVolumeSpec::setAwsElasticBlockStore)
        .visit(PersistentVolumeSpec::getAzureDisk, PersistentVolumeSpec::setAzureDisk)
        .visit(PersistentVolumeSpec::getAzureFile, PersistentVolumeSpec::setAzureFile)
        .visitMap(PersistentVolumeSpec::getCapacity, PersistentVolumeSpec::setCapacity)
        .visit(PersistentVolumeSpec::getCephfs, PersistentVolumeSpec::setCephfs)
        .visit(PersistentVolumeSpec::getCinder, PersistentVolumeSpec::setCinder)
        .visit(PersistentVolumeSpec::getCsi, PersistentVolumeSpec::setCsi)
        .visit(PersistentVolumeSpec::getFc, PersistentVolumeSpec::setFc)
        .visit(PersistentVolumeSpec::getFlexVolume, PersistentVolumeSpec::setFlexVolume)
        .visit(PersistentVolumeSpec::getFlocker, PersistentVolumeSpec::setFlocker)
        .visit(PersistentVolumeSpec::getGcePersistentDisk,
            PersistentVolumeSpec::setGcePersistentDisk)
        .visit(PersistentVolumeSpec::getGlusterfs, PersistentVolumeSpec::setGlusterfs)
        .visit(PersistentVolumeSpec::getHostPath, PersistentVolumeSpec::setHostPath)
        .visit(PersistentVolumeSpec::getIscsi, PersistentVolumeSpec::setIscsi)
        .visit(PersistentVolumeSpec::getLocal, PersistentVolumeSpec::setLocal)
        .visit(PersistentVolumeSpec::getNfs, PersistentVolumeSpec::setNfs)
        .visit(PersistentVolumeSpec::getPhotonPersistentDisk,
            PersistentVolumeSpec::setPhotonPersistentDisk)
        .visit(PersistentVolumeSpec::getPortworxVolume, PersistentVolumeSpec::setPortworxVolume)
        .visit(PersistentVolumeSpec::getQuobyte, PersistentVolumeSpec::setQuobyte)
        .visit(PersistentVolumeSpec::getRbd, PersistentVolumeSpec::setRbd)
        .visit(PersistentVolumeSpec::getScaleIO, PersistentVolumeSpec::setScaleIO)
        .visit(PersistentVolumeSpec::getStorageos, PersistentVolumeSpec::setStorageos)
        .visit(PersistentVolumeSpec::getVsphereVolume, PersistentVolumeSpec::setVsphereVolume)
        .visitMap(PersistentVolumeSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                PersistentVolumeSpec::getAdditionalProperties,
                PersistentVolumeSpec::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<VolumeNodeAffinity, T> visitVolumeNodeAffinity(
      PairVisitor<VolumeNodeAffinity, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(VolumeNodeAffinity::getRequired, VolumeNodeAffinity::setRequired,
            this::visitNodeSelector)
        .visitMap(VolumeNodeAffinity::getAdditionalProperties,
            additionalPropertiesSetter(
                VolumeNodeAffinity::getAdditionalProperties,
                VolumeNodeAffinity::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<NodeSelector, T> visitNodeSelector(PairVisitor<NodeSelector, T> pairVisitor) {
    return pairVisitor
        .visit()
        .visitListWith(NodeSelector::getNodeSelectorTerms,
            NodeSelector::setNodeSelectorTerms, this::visitNodeSelectorTerm)
        .visitMap(NodeSelector::getAdditionalProperties,
            additionalPropertiesSetter(
                NodeSelector::getAdditionalProperties,
                NodeSelector::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<NodeSelectorTerm, T> visitNodeSelectorTerm(
      PairVisitor<NodeSelectorTerm, T> pairVisitor) {
    return pairVisitor.visit()
        .visitListWith(NodeSelectorTerm::getMatchExpressions, NodeSelectorTerm::setMatchExpressions,
            this::visitNodeSelectorRequirement)
        .visitListWith(NodeSelectorTerm::getMatchFields, NodeSelectorTerm::setMatchFields,
            this::visitNodeSelectorRequirement)
        .visitMap(NodeSelectorTerm::getAdditionalProperties,
            additionalPropertiesSetter(
                NodeSelectorTerm::getAdditionalProperties,
                NodeSelectorTerm::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<NodeSelectorRequirement, T> visitNodeSelectorRequirement(
      PairVisitor<NodeSelectorRequirement, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(NodeSelectorRequirement::getKey, NodeSelectorRequirement::setKey)
        .visit(NodeSelectorRequirement::getOperator, NodeSelectorRequirement::setOperator)
        .visitList(NodeSelectorRequirement::getValues, NodeSelectorRequirement::setValues)
        .visitMap(NodeSelectorRequirement::getAdditionalProperties,
            additionalPropertiesSetter(
                NodeSelectorRequirement::getAdditionalProperties,
                NodeSelectorRequirement::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Service, T> visitService(PairVisitor<Service, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(Service::getSpec, Service::setSpec, this::visitServiceSpec)
        .visitMap(Service::getAdditionalProperties,
            additionalPropertiesSetter(
                Service::getAdditionalProperties,
                Service::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Endpoints, T> visitEndpoints(PairVisitor<Endpoints, T> pairVisitor) {
    return pairVisitor.visit()
        .visitMap(Endpoints::getAdditionalProperties,
            additionalPropertiesSetter(
                Endpoints::getAdditionalProperties,
                Endpoints::setAdditionalProperty))
        .visitListWith(Endpoints::getSubsets, Endpoints::setSubsets, this::visitEndpointSubset);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ServiceAccount, T> visitServiceAccount(
      PairVisitor<ServiceAccount, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ServiceAccount::getAutomountServiceAccountToken,
            ServiceAccount::setAutomountServiceAccountToken)
        .visitMap(ServiceAccount::getAdditionalProperties,
            additionalPropertiesSetter(
                ServiceAccount::getAdditionalProperties,
                ServiceAccount::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Role, T> visitRole(PairVisitor<Role, T> pairVisitor) {
    return pairVisitor.visit()
        .visitList(Role::getRules, Role::setRules)
        .visitMap(Role::getAdditionalProperties,
            additionalPropertiesSetter(
                Role::getAdditionalProperties,
                Role::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<RoleBinding, T> visitRoleBinding(PairVisitor<RoleBinding, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(RoleBinding::getRoleRef, RoleBinding::setRoleRef)
        .visitList(RoleBinding::getSubjects, RoleBinding::setSubjects)
        .visitMap(RoleBinding::getAdditionalProperties,
            additionalPropertiesSetter(
                RoleBinding::getAdditionalProperties,
                RoleBinding::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Secret, T> visitSecret(PairVisitor<Secret, T> pairVisitor) {
    return pairVisitor.visit()
        .transformRight(
            secret -> secret.getData() == null && secret.getStringData() != null
                ? new SecretBuilder(secret)
                    .withData(secret.getStringData().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                            e -> ResourceUtil.encodeSecret(e.getValue()))))
                    .withStringData(null).build()
                : secret)
        .visit(Secret::getType, Secret::setType)
        .visitMap(Secret::getData, Secret::setData)
        .visitMap(Secret::getAdditionalProperties,
            additionalPropertiesSetter(
                Secret::getAdditionalProperties,
                Secret::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ConfigMap, T> visitConfigMap(PairVisitor<ConfigMap, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ConfigMap::getAdditionalProperties,
            additionalPropertiesSetter(
                ConfigMap::getAdditionalProperties,
                ConfigMap::setAdditionalProperty))
        .visitMap(ConfigMap::getData, ConfigMap::setData)
        .visitMap(ConfigMap::getBinaryData, ConfigMap::setBinaryData)
        .visitMap(ConfigMap::getAdditionalProperties,
            additionalPropertiesSetter(
                ConfigMap::getAdditionalProperties,
                ConfigMap::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<StatefulSetSpec, T> visitStatefulSetSpec(
      PairVisitor<StatefulSetSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(StatefulSetSpec::getPodManagementPolicy, StatefulSetSpec::setPodManagementPolicy,
            "OrderedReady")
        .visit(StatefulSetSpec::getReplicas, StatefulSetSpec::setReplicas)
        .visit(StatefulSetSpec::getRevisionHistoryLimit, StatefulSetSpec::setRevisionHistoryLimit,
            10)
        .visit(StatefulSetSpec::getSelector, StatefulSetSpec::setSelector)
        .visit(StatefulSetSpec::getServiceName, StatefulSetSpec::setServiceName)
        .visit(StatefulSetSpec::getUpdateStrategy, StatefulSetSpec::setUpdateStrategy)
        .visitMap(StatefulSetSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                StatefulSetSpec::getAdditionalProperties,
                StatefulSetSpec::setAdditionalProperty))
        .visitListWith(StatefulSetSpec::getVolumeClaimTemplates,
            StatefulSetSpec::setVolumeClaimTemplates, this::visitPersistentVolumeClaim)
        .visitWith(StatefulSetSpec::getTemplate, StatefulSetSpec::setTemplate,
            this::visitPodTemplateSpec);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PersistentVolumeClaim, T> visitPersistentVolumeClaim(
      PairVisitor<PersistentVolumeClaim, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(PersistentVolumeClaim::getApiVersion, PersistentVolumeClaim::setApiVersion)
        .visit(PersistentVolumeClaim::getKind, PersistentVolumeClaim::setKind)
        .visitWith(PersistentVolumeClaim::getMetadata, PersistentVolumeClaim::setMetadata,
            this::visitMetadata)
        .visitMap(PersistentVolumeClaim::getAdditionalProperties,
            additionalPropertiesSetter(
                PersistentVolumeClaim::getAdditionalProperties,
                PersistentVolumeClaim::setAdditionalProperty))
        .visitWith(PersistentVolumeClaim::getSpec, PersistentVolumeClaim::setSpec,
            this::visitPersistentVolumeClaimSpec);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PersistentVolumeClaimSpec, T> visitPersistentVolumeClaimSpec(
      PairVisitor<PersistentVolumeClaimSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(PersistentVolumeClaimSpec::getDataSource, PersistentVolumeClaimSpec::setDataSource)
        .visitWith(PersistentVolumeClaimSpec::getSelector, PersistentVolumeClaimSpec::setSelector,
            this::visitLabelSelector)
        .visit(PersistentVolumeClaimSpec::getStorageClassName,
            PersistentVolumeClaimSpec::setStorageClassName)
        .visit(PersistentVolumeClaimSpec::getVolumeMode, PersistentVolumeClaimSpec::setVolumeMode,
            "Filesystem")
        .visitList(PersistentVolumeClaimSpec::getAccessModes,
            PersistentVolumeClaimSpec::setAccessModes)
        .visitMap(PersistentVolumeClaimSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                PersistentVolumeClaimSpec::getAdditionalProperties,
                PersistentVolumeClaimSpec::setAdditionalProperty))
        .visitWith(PersistentVolumeClaimSpec::getResources, PersistentVolumeClaimSpec::setResources,
            this::visitVolumeResourceRequirements);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<VolumeResourceRequirements, T> visitVolumeResourceRequirements(
      PairVisitor<VolumeResourceRequirements, T> pairVisitor) {
    return pairVisitor.visit()
        .visitMap(VolumeResourceRequirements::getAdditionalProperties,
            additionalPropertiesSetter(
                VolumeResourceRequirements::getAdditionalProperties,
                VolumeResourceRequirements::setAdditionalProperty))
        .visitMap(VolumeResourceRequirements::getLimits, VolumeResourceRequirements::setLimits)
        .visitMap(VolumeResourceRequirements::getRequests, VolumeResourceRequirements::setRequests);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ResourceRequirements, T> visitResourceRequirements(
      PairVisitor<ResourceRequirements, T> pairVisitor) {
    return pairVisitor.visit()
        .visitMap(ResourceRequirements::getAdditionalProperties,
            additionalPropertiesSetter(
                ResourceRequirements::getAdditionalProperties,
                ResourceRequirements::setAdditionalProperty))
        .visitMap(ResourceRequirements::getLimits, ResourceRequirements::setLimits)
        .visitMap(ResourceRequirements::getRequests, ResourceRequirements::setRequests);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodTemplateSpec, T> visitPodTemplateSpec(
      PairVisitor<PodTemplateSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(PodTemplateSpec::getMetadata, PodTemplateSpec::setMetadata, this::visitMetadata)
        .visitMap(PodTemplateSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                PodTemplateSpec::getAdditionalProperties,
                PodTemplateSpec::setAdditionalProperty))
        .visitWith(PodTemplateSpec::getSpec, PodTemplateSpec::setSpec, this::visitPodSpec);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodTemplateSpec, T> visitPodTemplateSpecFromJobSpec(
      PairVisitor<PodTemplateSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(PodTemplateSpec::getMetadata, PodTemplateSpec::setMetadata,
            this::visitMetadataWithoutLabels)
        .visitMap(PodTemplateSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                PodTemplateSpec::getAdditionalProperties,
                PodTemplateSpec::setAdditionalProperty))
        .visitWith(PodTemplateSpec::getSpec, PodTemplateSpec::setSpec, this::visitPodSpec);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodSpec, T> visitPodSpec(PairVisitor<PodSpec, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(PodSpec::getActiveDeadlineSeconds, PodSpec::setActiveDeadlineSeconds)
        .visit(PodSpec::getAutomountServiceAccountToken, PodSpec::setAutomountServiceAccountToken)
        .visitWith(PodSpec::getDnsConfig, PodSpec::setDnsConfig, this::visitPodDnsConfig)
        .visit(PodSpec::getDnsPolicy, PodSpec::setDnsPolicy, "ClusterFirst")
        .visit(PodSpec::getEnableServiceLinks, PodSpec::setEnableServiceLinks)
        .visit(PodSpec::getHostIPC, PodSpec::setHostIPC)
        .visit(PodSpec::getHostname, PodSpec::setHostname)
        .visit(PodSpec::getHostNetwork, PodSpec::setHostNetwork)
        .visit(PodSpec::getHostPID, PodSpec::setHostPID)
        .visit(PodSpec::getNodeName, PodSpec::setNodeName)
        .visit(PodSpec::getPriority, PodSpec::setPriority)
        .visit(PodSpec::getPriorityClassName, PodSpec::setPriorityClassName)
        .visit(PodSpec::getRestartPolicy, PodSpec::setRestartPolicy, "Always")
        .visit(PodSpec::getRuntimeClassName, PodSpec::setRuntimeClassName)
        .visit(PodSpec::getSchedulerName, PodSpec::setSchedulerName, "default-scheduler")
        .visitUsingDefaultFrom(PodSpec::getServiceAccount, PodSpec::setServiceAccount,
            PodSpec::getServiceAccountName)
        .visit(PodSpec::getServiceAccountName, PodSpec::setServiceAccountName)
        .visit(PodSpec::getShareProcessNamespace, PodSpec::setShareProcessNamespace)
        .visit(PodSpec::getSubdomain, PodSpec::setSubdomain)
        .visit(PodSpec::getTerminationGracePeriodSeconds, PodSpec::setTerminationGracePeriodSeconds,
            30L)
        .visitWithUsingDefaultFrom(PodSpec::getSecurityContext, PodSpec::setSecurityContext,
            this::visitPodSecurityContext, PodSecurityContext::new)
        .visitList(PodSpec::getHostAliases, PodSpec::setHostAliases)
        .visitList(PodSpec::getImagePullSecrets, PodSpec::setImagePullSecrets)
        .visitList(PodSpec::getReadinessGates, PodSpec::setReadinessGates)
        .visitList(PodSpec::getTolerations, PodSpec::setTolerations)
        .visitListWith(PodSpec::getVolumes, PodSpec::setVolumes, this::visitVolume)
        .visitMap(PodSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                PodSpec::getAdditionalProperties,
                PodSpec::setAdditionalProperty))
        .visitMap(PodSpec::getNodeSelector, PodSpec::setNodeSelector)
        .visitWith(PodSpec::getAffinity, PodSpec::setAffinity, this::visitAffinity)
        .visitListWith(PodSpec::getContainers, PodSpec::setContainers, this::visitContainer)
        .visitListWith(PodSpec::getInitContainers, PodSpec::setInitContainers,
            this::visitContainer);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodDNSConfig, T> visitPodDnsConfig(PairVisitor<PodDNSConfig, T> pairVisitor) {
    return pairVisitor.visit()
        .visitList(PodDNSConfig::getNameservers, PodDNSConfig::setNameservers)
        .visitList(PodDNSConfig::getSearches, PodDNSConfig::setSearches)
        .visitList(PodDNSConfig::getOptions, PodDNSConfig::setOptions)
        .visitMap(PodDNSConfig::getAdditionalProperties,
            additionalPropertiesSetter(
                PodDNSConfig::getAdditionalProperties,
                PodDNSConfig::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodSecurityContext, T> visitPodSecurityContext(
      PairVisitor<PodSecurityContext, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(PodSecurityContext::getFsGroup, PodSecurityContext::setFsGroup)
        .visit(PodSecurityContext::getRunAsGroup, PodSecurityContext::setRunAsGroup)
        .visit(PodSecurityContext::getRunAsNonRoot, PodSecurityContext::setRunAsNonRoot)
        .visit(PodSecurityContext::getRunAsUser, PodSecurityContext::setRunAsUser)
        .visit(PodSecurityContext::getSeLinuxOptions, PodSecurityContext::setSeLinuxOptions)
        .visitList(PodSecurityContext::getSupplementalGroups,
            PodSecurityContext::setSupplementalGroups)
        .visitList(PodSecurityContext::getSysctls, PodSecurityContext::setSysctls)
        .visitMap(PodSecurityContext::getAdditionalProperties,
            additionalPropertiesSetter(
                PodSecurityContext::getAdditionalProperties,
                PodSecurityContext::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Volume, T> visitVolume(PairVisitor<Volume, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(Volume::getName, Volume::setName)
        .visit(Volume::getAwsElasticBlockStore, Volume::setAwsElasticBlockStore)
        .visit(Volume::getAzureDisk, Volume::setAzureDisk)
        .visit(Volume::getAzureFile, Volume::setAzureFile)
        .visit(Volume::getCephfs, Volume::setCephfs).visit(Volume::getCinder, Volume::setCinder)
        .visitWith(Volume::getConfigMap, Volume::setConfigMap, this::visitConfigMapVolumeSource)
        .visit(Volume::getCsi, Volume::setCsi).visit(Volume::getDownwardAPI, Volume::setDownwardAPI)
        .visit(Volume::getEmptyDir, Volume::setEmptyDir).visit(Volume::getFc, Volume::setFc)
        .visit(Volume::getFlexVolume, Volume::setFlexVolume)
        .visit(Volume::getFlocker, Volume::setFlocker)
        .visit(Volume::getGcePersistentDisk, Volume::setGcePersistentDisk)
        .visit(Volume::getGitRepo, Volume::setGitRepo)
        .visit(Volume::getGlusterfs, Volume::setGlusterfs)
        .visit(Volume::getHostPath, Volume::setHostPath).visit(Volume::getIscsi, Volume::setIscsi)
        .visit(Volume::getNfs, Volume::setNfs)
        .visitWith(Volume::getPersistentVolumeClaim, Volume::setPersistentVolumeClaim,
            this::visitPersistentVolumeClaimVolumeSource)
        .visit(Volume::getPhotonPersistentDisk, Volume::setPhotonPersistentDisk)
        .visit(Volume::getPortworxVolume, Volume::setPortworxVolume)
        .visit(Volume::getProjected, Volume::setProjected)
        .visit(Volume::getQuobyte, Volume::setQuobyte).visit(Volume::getRbd, Volume::setRbd)
        .visit(Volume::getScaleIO, Volume::setScaleIO).visit(Volume::getSecret, Volume::setSecret)
        .visit(Volume::getStorageos, Volume::setStorageos)
        .visit(Volume::getVsphereVolume, Volume::setVsphereVolume)
        .visitMap(Volume::getAdditionalProperties,
            additionalPropertiesSetter(
                Volume::getAdditionalProperties,
                Volume::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ConfigMapVolumeSource, T> visitConfigMapVolumeSource(
      PairVisitor<ConfigMapVolumeSource, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ConfigMapVolumeSource::getName, ConfigMapVolumeSource::setName)
        .visit(ConfigMapVolumeSource::getDefaultMode, ConfigMapVolumeSource::setDefaultMode, 420)
        .visit(ConfigMapVolumeSource::getOptional, ConfigMapVolumeSource::setOptional)
        .visitList(ConfigMapVolumeSource::getItems, ConfigMapVolumeSource::setItems)
        .visitMap(ConfigMapVolumeSource::getAdditionalProperties,
            additionalPropertiesSetter(
                ConfigMapVolumeSource::getAdditionalProperties,
                ConfigMapVolumeSource::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PersistentVolumeClaimVolumeSource, T> visitPersistentVolumeClaimVolumeSource(
      PairVisitor<PersistentVolumeClaimVolumeSource, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(PersistentVolumeClaimVolumeSource::getClaimName,
            PersistentVolumeClaimVolumeSource::setClaimName)
        .visit(PersistentVolumeClaimVolumeSource::getReadOnly,
            PersistentVolumeClaimVolumeSource::setReadOnly, false)
        .visitMap(PersistentVolumeClaimVolumeSource::getAdditionalProperties,
            additionalPropertiesSetter(
                PersistentVolumeClaimVolumeSource::getAdditionalProperties,
                PersistentVolumeClaimVolumeSource::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Container, T> visitContainer(PairVisitor<Container, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(Container::getImage, Container::setImage)
        .visit(Container::getImagePullPolicy, Container::setImagePullPolicy, "Always")
        .visit(Container::getName, Container::setName)
        .visit(Container::getStdin, Container::setStdin)
        .visit(Container::getStdinOnce, Container::setStdinOnce)
        .visit(Container::getTerminationMessagePath, Container::setTerminationMessagePath,
            "/dev/termination-log")
        .visit(Container::getTerminationMessagePolicy, Container::setTerminationMessagePolicy,
            "File")
        .visit(Container::getTty, Container::setTty)
        .visit(Container::getWorkingDir, Container::setWorkingDir)
        .visit(Container::getSecurityContext, Container::setSecurityContext)
        .visit(Container::getLifecycle, Container::setLifecycle)
        .visitWith(Container::getLivenessProbe, Container::setLivenessProbe, this::visitProbe)
        .visitWith(Container::getReadinessProbe, Container::setReadinessProbe, this::visitProbe)
        .visitWithUsingDefaultFrom(Container::getResources, Container::setResources,
            this::visitResourceRequirements, ResourceRequirements::new)
        .visitList(Container::getArgs, Container::setArgs)
        .visitList(Container::getCommand, Container::setCommand)
        .visitListWith(Container::getEnv, Container::setEnv, this::visitEnvVar)
        .visitListWith(Container::getEnvFrom, Container::setEnvFrom, this::visitEnvFromSource)
        .visitListWith(Container::getPorts, Container::setPorts, this::visitContainerPort)
        .visitListWith(Container::getVolumeDevices, Container::setVolumeDevices,
            this::visitVolumeDevice)
        .visitListWith(Container::getVolumeMounts, Container::setVolumeMounts,
            this::visitVolumeMount)
        .visitMap(Container::getAdditionalProperties,
            additionalPropertiesSetter(
                Container::getAdditionalProperties,
                Container::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Probe, T> visitProbe(PairVisitor<Probe, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(Probe::getExec, Probe::setExec)
        .visit(Probe::getFailureThreshold, Probe::setFailureThreshold, 3)
        .visit(Probe::getHttpGet, Probe::setHttpGet)
        .visit(Probe::getInitialDelaySeconds, Probe::setInitialDelaySeconds)
        .visit(Probe::getPeriodSeconds, Probe::setPeriodSeconds, 10)
        .visit(Probe::getSuccessThreshold, Probe::setSuccessThreshold, 1)
        .visit(Probe::getTcpSocket, Probe::setTcpSocket)
        .visit(Probe::getTimeoutSeconds, Probe::setTimeoutSeconds, 1)
        .visitMap(Probe::getAdditionalProperties,
            additionalPropertiesSetter(
                Probe::getAdditionalProperties,
                Probe::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Affinity, T> visitAffinity(PairVisitor<Affinity, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(Affinity::getNodeAffinity, Affinity::setNodeAffinity, this::visitNodeAffinity)
        .visitWith(Affinity::getPodAffinity, Affinity::setPodAffinity, this::visitPodAffinity)
        .visitWith(Affinity::getPodAntiAffinity, Affinity::setPodAntiAffinity,
            this::visitPodAntiAffinity)
        .visitMap(Affinity::getAdditionalProperties,
            additionalPropertiesSetter(
                Affinity::getAdditionalProperties,
                Affinity::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<NodeAffinity, T> visitNodeAffinity(PairVisitor<NodeAffinity, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(NodeAffinity::getRequiredDuringSchedulingIgnoredDuringExecution,
            NodeAffinity::setRequiredDuringSchedulingIgnoredDuringExecution,
            this::visitNodeSelector)
        .visitListWith(NodeAffinity::getPreferredDuringSchedulingIgnoredDuringExecution,
            NodeAffinity::setPreferredDuringSchedulingIgnoredDuringExecution,
            this::visitPreferredSchedulingTerm)
        .visitMap(NodeAffinity::getAdditionalProperties,
            additionalPropertiesSetter(
                NodeAffinity::getAdditionalProperties,
                NodeAffinity::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PreferredSchedulingTerm, T> visitPreferredSchedulingTerm(
      PairVisitor<PreferredSchedulingTerm, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(PreferredSchedulingTerm::getWeight,
            PreferredSchedulingTerm::setWeight)
        .visitWith(PreferredSchedulingTerm::getPreference, PreferredSchedulingTerm::setPreference,
            this::visitNodeSelectorTerm)
        .visitMap(PreferredSchedulingTerm::getAdditionalProperties,
            additionalPropertiesSetter(
                PreferredSchedulingTerm::getAdditionalProperties,
                PreferredSchedulingTerm::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodAffinity, T> visitPodAffinity(PairVisitor<PodAffinity, T> pairVisitor) {
    return pairVisitor.visit()
        .visitList(PodAffinity::getPreferredDuringSchedulingIgnoredDuringExecution,
            PodAffinity::setPreferredDuringSchedulingIgnoredDuringExecution)
        .visitList(PodAffinity::getRequiredDuringSchedulingIgnoredDuringExecution,
            PodAffinity::setRequiredDuringSchedulingIgnoredDuringExecution)
        .visitMap(PodAffinity::getAdditionalProperties,
            additionalPropertiesSetter(
                PodAffinity::getAdditionalProperties,
                PodAffinity::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodAntiAffinity, T> visitPodAntiAffinity(
      PairVisitor<PodAntiAffinity, T> pairVisitor) {
    return pairVisitor.visit()
        .visitListWith(PodAntiAffinity::getPreferredDuringSchedulingIgnoredDuringExecution,
            PodAntiAffinity::setPreferredDuringSchedulingIgnoredDuringExecution,
            this::visitWeightedPodAffinityTerm)
        .visitListWith(PodAntiAffinity::getRequiredDuringSchedulingIgnoredDuringExecution,
            PodAntiAffinity::setRequiredDuringSchedulingIgnoredDuringExecution,
            this::visitPodAffinityTerm)
        .visitMap(PodAntiAffinity::getAdditionalProperties,
            additionalPropertiesSetter(
                PodAntiAffinity::getAdditionalProperties,
                PodAntiAffinity::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<WeightedPodAffinityTerm, T> visitWeightedPodAffinityTerm(
      PairVisitor<WeightedPodAffinityTerm, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(WeightedPodAffinityTerm::getPodAffinityTerm,
            WeightedPodAffinityTerm::setPodAffinityTerm,
            this::visitPodAffinityTerm)
        .visit(WeightedPodAffinityTerm::getWeight, WeightedPodAffinityTerm::setWeight)
        .visitMap(WeightedPodAffinityTerm::getAdditionalProperties,
            additionalPropertiesSetter(
                WeightedPodAffinityTerm::getAdditionalProperties,
                WeightedPodAffinityTerm::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<PodAffinityTerm, T> visitPodAffinityTerm(
      PairVisitor<PodAffinityTerm, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(PodAffinityTerm::getLabelSelector, PodAffinityTerm::setLabelSelector,
            this::visitLabelSelector)
        .visit(PodAffinityTerm::getTopologyKey, PodAffinityTerm::setTopologyKey)
        .visitList(PodAffinityTerm::getNamespaces, PodAffinityTerm::setNamespaces)
        .visitMap(PodAffinityTerm::getAdditionalProperties,
            additionalPropertiesSetter(
                PodAffinityTerm::getAdditionalProperties,
                PodAffinityTerm::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<LabelSelector, T> visitLabelSelector(
      PairVisitor<LabelSelector, T> pairVisitor) {
    return pairVisitor.visit()
        .visitList(LabelSelector::getMatchExpressions, LabelSelector::setMatchExpressions)
        .visitMap(LabelSelector::getMatchLabels, LabelSelector::setMatchLabels)
        .visitMap(LabelSelector::getAdditionalProperties,
            additionalPropertiesSetter(
                LabelSelector::getAdditionalProperties,
                LabelSelector::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<EnvVar, T> visitEnvVar(PairVisitor<EnvVar, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(EnvVar::getName, EnvVar::setName)
        .visit(EnvVar::getValue, EnvVar::setValue)
        .visitWith(EnvVar::getValueFrom, EnvVar::setValueFrom, this::visitEnvVarSource)
        .visitMap(EnvVar::getAdditionalProperties,
            additionalPropertiesSetter(
                EnvVar::getAdditionalProperties,
                EnvVar::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<EnvVarSource, T> visitEnvVarSource(PairVisitor<EnvVarSource, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(EnvVarSource::getConfigMapKeyRef, EnvVarSource::setConfigMapKeyRef,
            this::visitConfigMapKeySelector)
        .visitWith(EnvVarSource::getFieldRef, EnvVarSource::setFieldRef,
            this::visitObjectFieldSelector)
        .visitWith(EnvVarSource::getResourceFieldRef, EnvVarSource::setResourceFieldRef,
            this::visitResourceFieldSelector)
        .visitWith(EnvVarSource::getSecretKeyRef, EnvVarSource::setSecretKeyRef,
            this::visitSecretKeySelector)
        .visitMap(EnvVarSource::getAdditionalProperties,
            additionalPropertiesSetter(
                EnvVarSource::getAdditionalProperties,
                EnvVarSource::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ConfigMapKeySelector, T> visitConfigMapKeySelector(
      PairVisitor<ConfigMapKeySelector, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ConfigMapKeySelector::getKey, ConfigMapKeySelector::setKey)
        .visit(ConfigMapKeySelector::getName, ConfigMapKeySelector::setName)
        .visit(ConfigMapKeySelector::getOptional, ConfigMapKeySelector::setOptional)
        .visitMap(ConfigMapKeySelector::getAdditionalProperties,
            additionalPropertiesSetter(
                ConfigMapKeySelector::getAdditionalProperties,
                ConfigMapKeySelector::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ObjectFieldSelector, T> visitObjectFieldSelector(
      PairVisitor<ObjectFieldSelector, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ObjectFieldSelector::getApiVersion, ObjectFieldSelector::setApiVersion, "v1")
        .visit(ObjectFieldSelector::getFieldPath, ObjectFieldSelector::setFieldPath)
        .visitMap(ObjectFieldSelector::getAdditionalProperties,
            additionalPropertiesSetter(
                ObjectFieldSelector::getAdditionalProperties,
                ObjectFieldSelector::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ResourceFieldSelector, T> visitResourceFieldSelector(
      PairVisitor<ResourceFieldSelector, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ResourceFieldSelector::getContainerName, ResourceFieldSelector::setContainerName)
        .visit(ResourceFieldSelector::getResource, ResourceFieldSelector::setResource)
        .visitWith(ResourceFieldSelector::getDivisor, ResourceFieldSelector::setDivisor,
            this::visitQuantity)
        .visitMap(ResourceFieldSelector::getAdditionalProperties,
            additionalPropertiesSetter(
                ResourceFieldSelector::getAdditionalProperties,
                ResourceFieldSelector::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Quantity, T> visitQuantity(
      PairVisitor<Quantity, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(Quantity::getAmount, Quantity::setAmount)
        .visit(Quantity::getFormat, Quantity::setFormat)
        .visitMap(Quantity::getAdditionalProperties,
            additionalPropertiesSetter(
                Quantity::getAdditionalProperties,
                Quantity::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<SecretKeySelector, T> visitSecretKeySelector(
      PairVisitor<SecretKeySelector, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(SecretKeySelector::getKey, SecretKeySelector::setKey)
        .visit(SecretKeySelector::getName, SecretKeySelector::setName)
        .visit(SecretKeySelector::getOptional, SecretKeySelector::setOptional)
        .visitMap(SecretKeySelector::getAdditionalProperties,
            additionalPropertiesSetter(
                SecretKeySelector::getAdditionalProperties,
                SecretKeySelector::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<EnvFromSource, T> visitEnvFromSource(
      PairVisitor<EnvFromSource, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(EnvFromSource::getPrefix, EnvFromSource::setPrefix)
        .visitWith(EnvFromSource::getConfigMapRef, EnvFromSource::setConfigMapRef,
            this::visitConfigMapEnvSource)
        .visitWith(EnvFromSource::getSecretRef, EnvFromSource::setSecretRef,
            this::visitSecretEnvSource)
        .visitMap(EnvFromSource::getAdditionalProperties,
            additionalPropertiesSetter(
                EnvFromSource::getAdditionalProperties,
                EnvFromSource::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ConfigMapEnvSource, T> visitConfigMapEnvSource(
      PairVisitor<ConfigMapEnvSource, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ConfigMapEnvSource::getName, ConfigMapEnvSource::setName)
        .visit(ConfigMapEnvSource::getOptional, ConfigMapEnvSource::setOptional)
        .visitMap(ConfigMapEnvSource::getAdditionalProperties,
            additionalPropertiesSetter(
                ConfigMapEnvSource::getAdditionalProperties,
                ConfigMapEnvSource::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<SecretEnvSource, T> visitSecretEnvSource(
      PairVisitor<SecretEnvSource, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(SecretEnvSource::getName, SecretEnvSource::setName)
        .visit(SecretEnvSource::getOptional, SecretEnvSource::setOptional)
        .visitMap(SecretEnvSource::getAdditionalProperties,
            additionalPropertiesSetter(
                SecretEnvSource::getAdditionalProperties,
                SecretEnvSource::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ContainerPort, T> visitContainerPort(
      PairVisitor<ContainerPort, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ContainerPort::getContainerPort, ContainerPort::setContainerPort)
        .visit(ContainerPort::getHostIP, ContainerPort::setHostIP)
        .visit(ContainerPort::getHostPort, ContainerPort::setHostPort)
        .visit(ContainerPort::getName, ContainerPort::setName)
        .visit(ContainerPort::getProtocol, ContainerPort::setProtocol, "TCP")
        .visitMap(ContainerPort::getAdditionalProperties,
            additionalPropertiesSetter(
                ContainerPort::getAdditionalProperties,
                ContainerPort::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<VolumeDevice, T> visitVolumeDevice(PairVisitor<VolumeDevice, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(VolumeDevice::getName, VolumeDevice::setName)
        .visit(VolumeDevice::getDevicePath, VolumeDevice::setDevicePath)
        .visitMap(VolumeDevice::getAdditionalProperties,
            additionalPropertiesSetter(
                VolumeDevice::getAdditionalProperties,
                VolumeDevice::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<VolumeMount, T> visitVolumeMount(PairVisitor<VolumeMount, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(VolumeMount::getName, VolumeMount::setName)
        .visit(VolumeMount::getMountPath, VolumeMount::setMountPath)
        .visit(VolumeMount::getMountPropagation, VolumeMount::setMountPropagation)
        .visit(VolumeMount::getReadOnly, VolumeMount::setReadOnly, false)
        .visit(VolumeMount::getSubPath, VolumeMount::setSubPath)
        .visit(VolumeMount::getSubPathExpr, VolumeMount::setSubPathExpr)
        .visitMap(VolumeMount::getAdditionalProperties,
            additionalPropertiesSetter(
                VolumeMount::getAdditionalProperties,
                VolumeMount::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ServiceSpec, T> visitServiceSpec(PairVisitor<ServiceSpec, T> pairVisitor) {
    if (Optional.ofNullable(pairVisitor.right.getType())
        .filter("ExternalName"::equals)
        .isPresent()) {
      pairVisitor = pairVisitor.visit()
          .visit(ServiceSpec::getClusterIP, ServiceSpec::setClusterIP);
    }

    return pairVisitor.visit(ServiceSpec::getExternalName, ServiceSpec::setExternalName)
        .visit(ServiceSpec::getExternalTrafficPolicy, ServiceSpec::setExternalTrafficPolicy)
        .visit(ServiceSpec::getHealthCheckNodePort, ServiceSpec::setHealthCheckNodePort)
        .visit(ServiceSpec::getIpFamilyPolicy, ServiceSpec::setIpFamilyPolicy)
        .visit(ServiceSpec::getLoadBalancerIP, ServiceSpec::setLoadBalancerIP)
        .visit(ServiceSpec::getPublishNotReadyAddresses, ServiceSpec::setPublishNotReadyAddresses)
        .visit(ServiceSpec::getSessionAffinity, ServiceSpec::setSessionAffinity, "None")
        .visit(ServiceSpec::getSessionAffinityConfig, ServiceSpec::setSessionAffinityConfig)
        .visit(ServiceSpec::getType, ServiceSpec::setType, "ClusterIP")
        .visitList(ServiceSpec::getExternalIPs, ServiceSpec::setExternalIPs)
        .visitList(ServiceSpec::getIpFamilies, ServiceSpec::setIpFamilies)
        .visitList(ServiceSpec::getLoadBalancerSourceRanges,
            ServiceSpec::setLoadBalancerSourceRanges)
        .visitListWith(ServiceSpec::getPorts, ServiceSpec::setPorts, this::visitServicePort)
        .visitMap(ServiceSpec::getAdditionalProperties,
            additionalPropertiesSetter(
                ServiceSpec::getAdditionalProperties,
                ServiceSpec::setAdditionalProperty))
        .visitMap(ServiceSpec::getSelector, ServiceSpec::setSelector);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ServicePort, T> visitServicePort(PairVisitor<ServicePort, T> pairVisitor) {
    return pairVisitor.visit(
        ).visit(ServicePort::getName, ServicePort::setName)
        .visit(ServicePort::getNodePort, ServicePort::setNodePort)
        .visit(ServicePort::getPort, ServicePort::setPort)
        .visit(ServicePort::getNodePort, ServicePort::setNodePort)
        .visit(ServicePort::getProtocol, ServicePort::setProtocol, "TCP")
        .visitWithUsingDefaultFrom(ServicePort::getTargetPort, ServicePort::setTargetPort,
            this::visitIntOrString, () -> new IntOrString(pairVisitor.right.getPort()))
        .visitMap(ServicePort::getAdditionalProperties,
            additionalPropertiesSetter(
                ServicePort::getAdditionalProperties,
                ServicePort::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<IntOrString, T> visitIntOrString(PairVisitor<IntOrString, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(IntOrString::getIntVal, IntOrString::setValue)
        .visit(IntOrString::getStrVal, IntOrString::setValue);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<EndpointSubset, T> visitEndpointSubset(
      PairVisitor<EndpointSubset, T> pairVisitor) {
    return pairVisitor.visit()
        .visitList(EndpointSubset::getAddresses, EndpointSubset::setAddresses)
        .visitList(EndpointSubset::getNotReadyAddresses, EndpointSubset::setNotReadyAddresses)
        .visitList(EndpointSubset::getPorts, EndpointSubset::setPorts)
        .visitMap(EndpointSubset::getAdditionalProperties,
            additionalPropertiesSetter(
                EndpointSubset::getAdditionalProperties,
                EndpointSubset::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<Pod, T> visitPod(PairVisitor<Pod, T> pairVisitor) {
    return pairVisitor.visit()
        .visitWith(Pod::getSpec, Pod::setSpec, this::visitPodSpec)
        .visitMap(Pod::getAdditionalProperties,
            (objectMeta, map) -> map.forEach(pairVisitor.left::setAdditionalProperty));
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ObjectMeta, T> visitMetadata(PairVisitor<ObjectMeta, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ObjectMeta::getName)
        .visit(ObjectMeta::getNamespace)
        .visitListWith(ObjectMeta::getOwnerReferences, ObjectMeta::setOwnerReferences,
            this::visitOwnerReference)
        .visitMapTransformed(ObjectMeta::getAnnotations, ObjectMeta::setAnnotations,
            this::leftAnnotationTransformer, this::rightAnnotationTransformer,
            HashMap<String, String>::new)
        .visitMap(ObjectMeta::getAdditionalProperties,
            additionalPropertiesSetter(
                ObjectMeta::getAdditionalProperties,
                ObjectMeta::setAdditionalProperty))
        .visitMap(ObjectMeta::getLabels, ObjectMeta::setLabels);
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<OwnerReference, T> visitOwnerReference(
      PairVisitor<OwnerReference, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(OwnerReference::getKind, OwnerReference::setKind)
        .visit(OwnerReference::getUid, OwnerReference::setUid)
        .visit(OwnerReference::getName, OwnerReference::setName)
        .visit(OwnerReference::getController, OwnerReference::setController)
        .visit(OwnerReference::getBlockOwnerDeletion, OwnerReference::setBlockOwnerDeletion)
        .visitMap(OwnerReference::getAdditionalProperties,
            additionalPropertiesSetter(
                OwnerReference::getAdditionalProperties,
                OwnerReference::setAdditionalProperty));
  }

  protected Map.Entry<String, String> leftAnnotationTransformer(
      Map.Entry<String, String> leftAnnotation,
      Map.Entry<String, String> rightAnnotation) {
    return leftAnnotation;
  }

  protected Map.Entry<String, String> rightAnnotationTransformer(
      Map.Entry<String, String> leftAnnotation,
      Map.Entry<String, String> rightAnnotation) {
    if (rightAnnotation == null) {
      return leftAnnotation;
    }
    return rightAnnotation;
  }

  /**
   * Visit using a pair visitor.
   */
  public PairVisitor<ObjectMeta, T> visitMetadataWithoutLabels(
      PairVisitor<ObjectMeta, T> pairVisitor) {
    return pairVisitor.visit()
        .visit(ObjectMeta::getName).visit(ObjectMeta::getNamespace)
        .visitMap(ObjectMeta::getAdditionalProperties,
            additionalPropertiesSetter(
                ObjectMeta::getAdditionalProperties,
                ObjectMeta::setAdditionalProperty));
  }

  protected interface AdditionalPropertySetter<O> {
    void setAdditionalProperty(O object, String key, Object value);
  }

  protected <O> BiConsumer<O, Map<String, Object>> additionalPropertiesSetter(
      Function<O, Map<String, Object>> additionalPropertiesGetter,
      AdditionalPropertySetter<O> additionalPropertySetter) {
    return (O object, Map<String, Object> map) -> {
      map.forEach((key, value) -> additionalPropertySetter
          .setAdditionalProperty(object, key, value));
      Map<String, Object> additionalProperties = additionalPropertiesGetter.apply(object);
      new HashMap<>(additionalProperties).entrySet().stream()
        .filter(additionalProperty -> !map.containsKey(additionalProperty.getKey()))
        .forEach(additionalProperty -> additionalProperties.remove(additionalProperty.getKey()));
    };
  }
}
