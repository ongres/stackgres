/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.AWSElasticBlockStoreVolumeSource;
import io.fabric8.kubernetes.api.model.AzureDiskVolumeSource;
import io.fabric8.kubernetes.api.model.AzureFileVolumeSource;
import io.fabric8.kubernetes.api.model.CSIVolumeSource;
import io.fabric8.kubernetes.api.model.CephFSVolumeSource;
import io.fabric8.kubernetes.api.model.CinderVolumeSource;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSource;
import io.fabric8.kubernetes.api.model.DownwardAPIVolumeSource;
import io.fabric8.kubernetes.api.model.EmptyDirVolumeSource;
import io.fabric8.kubernetes.api.model.EphemeralVolumeSource;
import io.fabric8.kubernetes.api.model.FCVolumeSource;
import io.fabric8.kubernetes.api.model.FlexVolumeSource;
import io.fabric8.kubernetes.api.model.FlockerVolumeSource;
import io.fabric8.kubernetes.api.model.GCEPersistentDiskVolumeSource;
import io.fabric8.kubernetes.api.model.GitRepoVolumeSource;
import io.fabric8.kubernetes.api.model.GlusterfsVolumeSource;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.ISCSIVolumeSource;
import io.fabric8.kubernetes.api.model.ImageVolumeSource;
import io.fabric8.kubernetes.api.model.NFSVolumeSource;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource;
import io.fabric8.kubernetes.api.model.PhotonPersistentDiskVolumeSource;
import io.fabric8.kubernetes.api.model.PortworxVolumeSource;
import io.fabric8.kubernetes.api.model.ProjectedVolumeSource;
import io.fabric8.kubernetes.api.model.QuobyteVolumeSource;
import io.fabric8.kubernetes.api.model.RBDVolumeSource;
import io.fabric8.kubernetes.api.model.ScaleIOVolumeSource;
import io.fabric8.kubernetes.api.model.SecretVolumeSource;
import io.fabric8.kubernetes.api.model.StorageOSVolumeSource;
import io.fabric8.kubernetes.api.model.VsphereVirtualDiskVolumeSource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.sundr.builder.annotations.Buildable;
import io.sundr.builder.annotations.BuildableReference;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder",
    refs = {
      @BuildableReference(io.fabric8.kubernetes.api.model.Volume.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.AWSElasticBlockStoreVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.AzureDiskVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.AzureFileVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.CephFSVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.CinderVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.ConfigMapVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.CSIVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.DownwardAPIVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.EmptyDirVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.EphemeralVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.FCVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.FlexVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.FlockerVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.GCEPersistentDiskVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.GitRepoVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.GlusterfsVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.HostPathVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.ImageVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.ISCSIVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.NFSVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.PhotonPersistentDiskVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.PortworxVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.ProjectedVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.QuobyteVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.RBDVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.ScaleIOVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.SecretVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.StorageOSVolumeSource.class),
      @BuildableReference(io.fabric8.kubernetes.api.model.VsphereVirtualDiskVolumeSource.class),
    })
public class CustomVolume extends io.fabric8.kubernetes.api.model.Volume {

  private static final long serialVersionUID = 1L;

  public CustomVolume() {
    super();
  }

  //CHECKSTYLE:OFF
  public CustomVolume(AWSElasticBlockStoreVolumeSource awsElasticBlockStore,
      AzureDiskVolumeSource azureDisk, AzureFileVolumeSource azureFile, CephFSVolumeSource cephfs,
      CinderVolumeSource cinder, ConfigMapVolumeSource configMap, CSIVolumeSource csi,
      DownwardAPIVolumeSource downwardAPI, EmptyDirVolumeSource emptyDir,
      EphemeralVolumeSource ephemeral, FCVolumeSource fc, FlexVolumeSource flexVolume,
      FlockerVolumeSource flocker, GCEPersistentDiskVolumeSource gcePersistentDisk,
      GitRepoVolumeSource gitRepo, GlusterfsVolumeSource glusterfs, HostPathVolumeSource hostPath,
      ImageVolumeSource image, ISCSIVolumeSource iscsi, String name, NFSVolumeSource nfs,
      PersistentVolumeClaimVolumeSource persistentVolumeClaim,
      PhotonPersistentDiskVolumeSource photonPersistentDisk, PortworxVolumeSource portworxVolume,
      ProjectedVolumeSource projected, QuobyteVolumeSource quobyte, RBDVolumeSource rbd,
      ScaleIOVolumeSource scaleIO, SecretVolumeSource secret, StorageOSVolumeSource storageos,
      VsphereVirtualDiskVolumeSource vsphereVolume) {
    super(awsElasticBlockStore, azureDisk, azureFile, cephfs, cinder, configMap, csi, downwardAPI,
        emptyDir, ephemeral, fc, flexVolume, flocker, gcePersistentDisk, gitRepo, glusterfs, hostPath,
        image, iscsi, name, nfs, persistentVolumeClaim, photonPersistentDisk, portworxVolume, projected,
        quobyte, rbd, scaleIO, secret, storageos, vsphereVolume);
  }

  @Override
  public AWSElasticBlockStoreVolumeSource getAwsElasticBlockStore() {
    return super.getAwsElasticBlockStore();
  }

  @Override
  public void setAwsElasticBlockStore(AWSElasticBlockStoreVolumeSource awsElasticBlockStore) {
    super.setAwsElasticBlockStore(awsElasticBlockStore);
  }

  @Override
  public AzureDiskVolumeSource getAzureDisk() {
    return super.getAzureDisk();
  }

  @Override
  public void setAzureDisk(AzureDiskVolumeSource azureDisk) {
    super.setAzureDisk(azureDisk);
  }

  @Override
  public AzureFileVolumeSource getAzureFile() {
    return super.getAzureFile();
  }

  @Override
  public void setAzureFile(AzureFileVolumeSource azureFile) {
    super.setAzureFile(azureFile);
  }

  @Override
  public CephFSVolumeSource getCephfs() {
    return super.getCephfs();
  }

  @Override
  public void setCephfs(CephFSVolumeSource cephfs) {
    super.setCephfs(cephfs);
  }

  @Override
  public CinderVolumeSource getCinder() {
    return super.getCinder();
  }

  @Override
  public void setCinder(CinderVolumeSource cinder) {
    super.setCinder(cinder);
  }

  @Override
  public ConfigMapVolumeSource getConfigMap() {
    return super.getConfigMap();
  }

  @Override
  public void setConfigMap(ConfigMapVolumeSource configMap) {
    super.setConfigMap(configMap);
  }

  @Override
  public CSIVolumeSource getCsi() {
    return super.getCsi();
  }

  @Override
  public void setCsi(CSIVolumeSource csi) {
    super.setCsi(csi);
  }

  @Override
  public DownwardAPIVolumeSource getDownwardAPI() {
    return super.getDownwardAPI();
  }

  @Override
  public void setDownwardAPI(DownwardAPIVolumeSource downwardAPI) {
    super.setDownwardAPI(downwardAPI);
  }

  @Override
  public EmptyDirVolumeSource getEmptyDir() {
    return super.getEmptyDir();
  }

  @Override
  public void setEmptyDir(EmptyDirVolumeSource emptyDir) {
    super.setEmptyDir(emptyDir);
  }

  @Override
  public EphemeralVolumeSource getEphemeral() {
    return super.getEphemeral();
  }

  @Override
  public void setEphemeral(EphemeralVolumeSource ephemeral) {
    super.setEphemeral(ephemeral);
  }

  @Override
  public FCVolumeSource getFc() {
    return super.getFc();
  }

  @Override
  public void setFc(FCVolumeSource fc) {
    super.setFc(fc);
  }

  @Override
  public FlexVolumeSource getFlexVolume() {
    return super.getFlexVolume();
  }

  @Override
  public void setFlexVolume(FlexVolumeSource flexVolume) {
    super.setFlexVolume(flexVolume);
  }

  @Override
  public FlockerVolumeSource getFlocker() {
    return super.getFlocker();
  }

  @Override
  public void setFlocker(FlockerVolumeSource flocker) {
    super.setFlocker(flocker);
  }

  @Override
  public GCEPersistentDiskVolumeSource getGcePersistentDisk() {
    return super.getGcePersistentDisk();
  }

  @Override
  public void setGcePersistentDisk(GCEPersistentDiskVolumeSource gcePersistentDisk) {
    super.setGcePersistentDisk(gcePersistentDisk);
  }

  @Override
  public GitRepoVolumeSource getGitRepo() {
    return super.getGitRepo();
  }

  @Override
  public void setGitRepo(GitRepoVolumeSource gitRepo) {
    super.setGitRepo(gitRepo);
  }

  @Override
  public GlusterfsVolumeSource getGlusterfs() {
    return super.getGlusterfs();
  }

  @Override
  public void setGlusterfs(GlusterfsVolumeSource glusterfs) {
    super.setGlusterfs(glusterfs);
  }

  @Override
  public HostPathVolumeSource getHostPath() {
    return super.getHostPath();
  }

  @Override
  public void setHostPath(HostPathVolumeSource hostPath) {
    super.setHostPath(hostPath);
  }

  @Override
  public ImageVolumeSource getImage() {
    return super.getImage();
  }

  @Override
  public void setImage(ImageVolumeSource iscsi) {
    super.setImage(iscsi);
  }

  @Override
  public ISCSIVolumeSource getIscsi() {
    return super.getIscsi();
  }

  @Override
  public void setIscsi(ISCSIVolumeSource iscsi) {
    super.setIscsi(iscsi);
  }

  @Override
  public String getName() {
    return super.getName();
  }

  @Override
  public void setName(String name) {
    super.setName(name);
  }

  @Override
  public NFSVolumeSource getNfs() {
    return super.getNfs();
  }

  @Override
  public void setNfs(NFSVolumeSource nfs) {
    super.setNfs(nfs);
  }

  @Override
  public PersistentVolumeClaimVolumeSource getPersistentVolumeClaim() {
    return super.getPersistentVolumeClaim();
  }

  @Override
  public void setPersistentVolumeClaim(PersistentVolumeClaimVolumeSource persistentVolumeClaim) {
    super.setPersistentVolumeClaim(persistentVolumeClaim);
  }

  @Override
  public PhotonPersistentDiskVolumeSource getPhotonPersistentDisk() {
    return super.getPhotonPersistentDisk();
  }

  @Override
  public void setPhotonPersistentDisk(PhotonPersistentDiskVolumeSource photonPersistentDisk) {
    super.setPhotonPersistentDisk(photonPersistentDisk);
  }

  @Override
  public PortworxVolumeSource getPortworxVolume() {
    return super.getPortworxVolume();
  }

  @Override
  public void setPortworxVolume(PortworxVolumeSource portworxVolume) {
    super.setPortworxVolume(portworxVolume);
  }

  @Override
  public ProjectedVolumeSource getProjected() {
    return super.getProjected();
  }

  @Override
  public void setProjected(ProjectedVolumeSource projected) {
    super.setProjected(projected);
  }

  @Override
  public QuobyteVolumeSource getQuobyte() {
    return super.getQuobyte();
  }

  @Override
  public void setQuobyte(QuobyteVolumeSource quobyte) {
    super.setQuobyte(quobyte);
  }

  @Override
  public RBDVolumeSource getRbd() {
    return super.getRbd();
  }

  @Override
  public void setRbd(RBDVolumeSource rbd) {
    super.setRbd(rbd);
  }

  @Override
  public ScaleIOVolumeSource getScaleIO() {
    return super.getScaleIO();
  }

  @Override
  public void setScaleIO(ScaleIOVolumeSource scaleIO) {
    super.setScaleIO(scaleIO);
  }

  @Override
  public SecretVolumeSource getSecret() {
    return super.getSecret();
  }

  @Override
  public void setSecret(SecretVolumeSource secret) {
    super.setSecret(secret);
  }

  @Override
  public StorageOSVolumeSource getStorageos() {
    return super.getStorageos();
  }

  @Override
  public void setStorageos(StorageOSVolumeSource storageos) {
    super.setStorageos(storageos);
  }

  @Override
  public VsphereVirtualDiskVolumeSource getVsphereVolume() {
    return super.getVsphereVolume();
  }

  @Override
  public void setVsphereVolume(VsphereVirtualDiskVolumeSource vsphereVolume) {
    super.setVsphereVolume(vsphereVolume);
  }
  //CHECKSTYLE:ON

}
