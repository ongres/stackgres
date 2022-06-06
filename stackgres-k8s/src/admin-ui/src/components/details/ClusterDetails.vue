<template>
	<div id="clusterDetails">
        <template v-if="cluster.hasOwnProperty('data')">
            <table class="clusterConfig">
                <tbody>
                    <tr>
                        <td class="label">
                            Cluster Namespace
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.metadata.namespace')"></span>
                        </td>
                        <td colspan="3">
                            <router-link :to="'/' + cluster.data.metadata.namespace">
                                {{ cluster.data.metadata.namespace }}
                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                            </router-link>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Cluster Name
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.name')"></span>
                        </td>
                        <td colspan="3">
                            <router-link :to="'/' + cluster.data.metadata.namespace + '/sgcluster/' + cluster.data.metadata.name">
                                {{ cluster.data.metadata.name }}
                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                            </router-link>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Postgres Flavor
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.flavor')"></span>
                        </td>
                        <td colspan="3" class="capitalize">{{ hasProp(cluster, 'data.spec.postgres.flavor') ? cluster.data.spec.postgres.flavor : 'vanilla' }}</td>
                    </tr>
                    <tr>
                        <td class="label">
                            Postgres Version
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgres.version')"></span>
                        </td>
                        <td colspan="3">{{ cluster.data.spec.postgres.version }}</td>
                    </tr>
                    <tr>
                        <td class="label">
                            Instances
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
                        </td>
                        <td colspan="3" class="textRight">{{ cluster.data.spec.instances }}</td>
                    </tr>
                    <tr>
                        <td class="label">
                            Instance Profile
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.sgInstanceProfile')"></span>
                        </td>
                        <template v-if="hasProp(cluster, 'data.spec.sgInstanceProfile')">
                            <td colspan="3" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
                                <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + cluster.data.spec.sgInstanceProfile">
                                    {{ cluster.data.spec.sgInstanceProfile }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }}) 
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                </router-link>
                                <template v-if="clusterProfileMismatch(cluster, profile)">
                                    <span class="helpTooltip alert" data-tooltip="This profile has been modified recently. Cluster must be restarted in order to apply such changes."></span>
                                </template>
                            </td>
                        </template>
                        <template v-else>
                            <td colspan="3">
                                Default (Cores: 1, RAM: 2GiB)
                            </td>
                        </template>
                    </tr>
                    <tr>
                        <td class="label" rowspan="3">
                            Pods
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods')"></span>
                        </td>
                        <td class="label" :rowspan="Object.keys(cluster.data.spec.pods.persistentVolume).length">
                            Persistent Volume
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume')"></span>
                        </td>
                        <td class="label">
                            Volume Size
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.size')"></span>
                        </td>
                        <td class="textRight">{{ cluster.data.spec.pods.persistentVolume.size }}</td>
                    </tr>
                    <tr v-if="(typeof cluster.data.spec.pods.persistentVolume.storageClass !== 'undefined')">
                        <td class="label">
                            Storage Class
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.storageClass')"></span>
                        </td>
                        <td>{{ cluster.data.spec.pods.persistentVolume.storageClass }}</td>
                    </tr>
                    <tr>
                        <td class="label">
                            Connection Pooling
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                        </td>
                        <td colspan="2">
                            <template v-if="hasProp(cluster, 'data.spec.pods.disableConnectionPooling') && cluster.data.spec.pods.disableConnectionPooling">
                                OFF
                            </template>
                            <template v-else>
                                ON
                            </template>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Metrics Exporter
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                        </td>
                        <td colspan="3">
                            <template v-if="!cluster.data.spec.pods.disableMetricsExporter">
                                ON
                            </template>
                            <template v-else>
                                OFF
                            </template>
                        </td>
                    </tr>
                    <tr>
                        <td class="label" :rowspan="( Object.keys(cluster.data.spec.replication).length + (cluster.data.spec.replication.hasOwnProperty('groups') && (cluster.data.spec.replication.groups.length - 1) ) )">
                            Replication
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication')"></span>
                        </td>
                        <td class="label">
                            Role
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.role')"></span>
                        </td>
                        <td colspan="2">
                            {{ cluster.data.spec.replication.role }}
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Mode
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.mode')"></span>
                        </td>
                        <td colspan="2">
                            {{ cluster.data.spec.replication.mode }}
                        </td>
                    </tr>
                    <tr v-if="cluster.data.spec.replication.hasOwnProperty('syncInstances')">
                        <td class="label">
                            Sync Node Count
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.syncInstances')"></span>
                        </td>
                        <td colspan="2">
                            {{ cluster.data.spec.replication.syncInstances }}
                        </td>
                    </tr>
                    <template v-if="cluster.data.spec.replication.hasOwnProperty('groups')">
                        <tr v-for="(group, index) in cluster.data.spec.replication.groups">
                            <td
                                class="label"
                                :rowspan="cluster.data.spec.replication.groups.length"
                                v-if="!index">
                                Groups
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replication.groups')"></span>
                            </td>
                            <td>
                                Group #{{ index+1 }}
                            </td>
                            <td>
                                <ul class="tableCells">
                                    <li v-for="(value, key) in group">
                                        <span class="label capitalize">
                                            {{ key }}
                                        </span>
                                        <span>
                                            {{ value }}
                                        </span>
                                    </li>
                                </ul>
                            </td>
                        </tr>
                    </template>
                    <tr>
                        <td class="label">
                            Monitoring
                            <span class="helpTooltip" data-tooltip="StackGres supports enabling automatic monitoring for your Postgres cluster, but you need to provide or install the <a href='https://stackgres.io/doc/latest/install/prerequisites/monitoring/' target='_blank'>Prometheus stack as a pre-requisite</a>. Then, check this option to configure automatically sending metrics to the Prometheus stack."></span>
                        </td>
                        <td colspan="3">
                            <template v-if="(typeof cluster.data.spec.prometheusAutobind !== 'undefined') && !cluster.data.spec.pods.disableMetricsExporter">
                                ON
                            </template>
                            <template v-else>
                                OFF
                            </template>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Prometheus Autobind
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
                        </td>
                        <td colspan="3">
                            <template v-if="(typeof cluster.data.spec.prometheusAutobind !== 'undefined')">
                                ON
                            </template>
                            <template v-else>
                                OFF
                            </template>
                        </td>
                    </tr>
                    <tr v-if="(typeof cluster.data.spec.nonProductionOptions !== 'undefined')">
                        <td class="label">
                            Non-Production Settings
                        </td>
                        <td class="label">
                            Cluster Pod Anti Affinity
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('Set this property to true to allow','When disabled, it allows running')"></span>
                        </td>
                        <td colspan="2">
                            <template v-if="(typeof cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity !== 'undefined')">
                                {{ cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity ? 'OFF' : 'ON' }}
                            </template>
                            <template v-else>
                                OFF
                            </template>
                        </td>
                    </tr>
                    <tr v-if="hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                        <td class="label" :rowspan="Object.keys(cluster.data.spec.distributedLogs).length">
                            Distributed Logs
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.distributedLogs')"></span>
                        </td>
                        <td class="label">
                            Logs Server
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.sgDistributedLogs')"></span>
                        </td>
                        <td colspan="2">
                            <router-link :to="'/' + (cluster.data.spec.distributedLogs.sgDistributedLogs.includes('.') ? cluster.data.spec.distributedLogs.sgDistributedLogs.split('.')[0] : $route.params.namespace) + '/sgdistributedlog/' +  (cluster.data.spec.distributedLogs.sgDistributedLogs.includes('.') ? cluster.data.spec.distributedLogs.sgDistributedLogs.split('.')[1] : cluster.data.spec.distributedLogs.sgDistributedLogs)">
                                {{ 
                                    (
                                        cluster.data.spec.distributedLogs.sgDistributedLogs.includes('.') ? 
                                            (cluster.data.spec.distributedLogs.sgDistributedLogs.split('.')[0] + ' > ' + cluster.data.spec.distributedLogs.sgDistributedLogs.split('.')[1]) : 
                                            cluster.data.spec.distributedLogs.sgDistributedLogs
                                    ) 
                                }}
                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                            </router-link>
                        </td>
                    </tr>
                    <tr v-if="hasProp(cluster, 'data.spec.distributedLogs.retention')">
                        <td class="label">
                            Retention
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.retention')"></span>
                        </td>
                        <td colspan="2">
                            {{ cluster.data.spec.distributedLogs.retention }}
                        </td>
                    </tr>
                    <template v-if="hasProp(cluster, 'data.spec.initialData.restore')">
                        <tr :set="backup = backups.find( b => (b.data.metadata.name == cluster.data.spec.initialData.restore.fromBackup.name) )">
                            <td class="label" :rowspan="1 + Object.keys(cluster.data.spec.initialData.restore.fromBackup).length + ( (typeof backup !== 'undefined') ? 1 : 0)">
                                Initial Data
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData')"></span>
                            </td>
                            <td class="label">
                                Download Disk Concurrency
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.downloadDiskConcurrency')"></span>
                            </td>
                            <td colspan="2" class="textRight">
                                {{ hasProp(cluster , 'data.spec.initialData.restore.downloadDiskConcurrency') ? cluster.data.spec.initialData.restore.downloadDiskConcurrency : '1' }}
                            </td>
                        </tr>
                        <tr>
                            <td class="label" :rowspan="(cluster.data.spec.initialData.restore.fromBackup.hasOwnProperty('pointInTimeRecovery') ? 1 : 0) + ((typeof backup !== 'undefined') ? 2 : 1)">
                                Restore from Backup
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup')"></span>
                            </td>
                            <td class="label">
                                Backup Name
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.name')"></span>
                            </td>
                            <td>
                                <template v-if="(typeof backup !== 'undefined')">
                                    <router-link :to="'/' + $route.params.namespace + '/sgbackup/' + backup.data.metadata.name"> 
                                        {{ cluster.data.spec.initialData.restore.fromBackup.name }}
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                    </router-link>
                                </template>
                                <template v-else>
                                    {{ cluster.data.spec.initialData.restore.fromBackup.name }}
                                </template>
                            </td>
                        </tr>
                        <tr v-if="(typeof backup !== 'undefined')">
                            <td class="label">
                                Date
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.stored')"></span>
                            </td>
                            <td class="timestamp">
                                <template v-if="backup.data.status.process.status == 'Completed'">
                                    <span class='date'>
                                        {{ backup.data.status.process.timing.stored | formatTimestamp('date') }}
                                    </span>
                                    <span class='time'>
                                        {{ backup.data.status.process.timing.stored | formatTimestamp('time') }}
                                    </span>
                                    <span class='ms'>
                                        {{ backup.data.status.process.timing.stored | formatTimestamp('ms') }}
                                    </span>
                                    <span class='tzOffset'>{{ showTzOffset() }}</span>
                                </template>
                            </td>
                        </tr>
                        <tr v-if="cluster.data.spec.initialData.restore.fromBackup.hasOwnProperty('pointInTimeRecovery')">
                            <td class="label">
                                Point-in-Time Recovery
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.initialData.restore.fromBackup.pointInTimeRecovery')"></span>
                            </td>
                            <td class="timestamp">
                                <span class='date'>
                                    {{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('date') }}
                                </span>
                                <span class='time'>
                                    {{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('time') }}
                                </span>
                                <span class='ms'>
                                    {{ cluster.data.spec.initialData.restore.fromBackup.pointInTimeRecovery.restoreToTimestamp | formatTimestamp('ms') }}
                                </span>
                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                            </td>
                        </tr>
                    </template>	
                </tbody>
            </table>

            <div class="clusterConfigurations">
                <h2>Cluster Configurations <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations')"></span></h2>
                <table class="clusterConfig">
                    <tbody>
                        <tr>
                            <td class="label">
                                Postgres
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPostgresConfig')"></span>
                            </td>
                            <template v-if="hasProp(cluster, 'data.spec.configurations.sgPostgresConfig')">
                                <td colspan="2">
                                    <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.configurations.sgPostgresConfig">
                                        {{ cluster.data.spec.configurations.sgPostgresConfig }}
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                    </router-link>								
                                </td>
                            </template>
                            <template v-else>
                                <td colspan="2">
                                    Default
                                </td>
                            </template>
                        </tr>
                        <tr>
                            <td class="label">
                                Connection Pooling
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                            </td>
                            <template v-if="hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                <td colspan="2">
                                    <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.configurations.sgPoolingConfig">
                                        {{ cluster.data.spec.configurations.sgPoolingConfig }}
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                    </router-link>	
                                </td>
                            </template>
                            <template v-else-if="!hasProp(cluster, 'data.spec.pods.disableConnectionPooling') || !cluster.data.spec.pods.disableConnectionPooling">
                                <td colspan="2">
                                    Default
                                </td>
                            </template>
                        </tr>
                        <tr v-if="hasProp(cluster, 'data.spec.configurations.sgBackupConfig')">
                            <td class="label">
                                Managed Backups
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.sgBackupConfig')"></span>
                            </td>
                            <td colspan="2">
                                <router-link :to="'/' + $route.params.namespace + '/sgbackupconfig/' + cluster.data.spec.configurations.sgBackupConfig">
                                    {{ cluster.data.spec.configurations.sgBackupConfig }}
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                </router-link>
                            </td>
                        </tr>
                        <tr v-if="hasProp(cluster, 'data.spec.configurations.backupPath')">
                            <td class="label">
                                Backup Path
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.configurations.backupPath')"></span>
                            </td>
                            <td colspan="2">
                                {{ cluster.data.spec.configurations.backupPath }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="podsScheduling" v-if="hasProp(cluster, 'data.spec.pods.scheduling')">
                <h2>Pods Scheduling <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling')"></span></h2>
                <table class="clusterConfig">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.pods.scheduling.nodeSelector)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.pods.scheduling.nodeSelector).length">
                                Node selectors
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeSelector')"></span>
                            </td>
                            <td class="label">
                                {{ item.annotation }}
                            </td>
                            <td colspan="2">
                                {{ item.value }}
                            </td>
                        </tr>
                        <template v-for="(item, index) in cluster.data.spec.pods.scheduling.tolerations">
                            <tr v-for="(value, prop, i) in item">
                                <td v-if="!index && !i" class="label" :rowspan="countObjectArrayKeys(cluster.data.spec.pods.scheduling.tolerations)">
                                    Tolerations
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations')"></span>
                                </td>
                                <td class="label" :rowspan="Object.keys(item).length" v-if="!i">
                                    Toleration #{{ index+1 }}
                                </td>
                                <td class="label">
                                    {{ prop }}
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.tolerations.' + prop)"></span>
                                </td>
                                <td colspan="2" :class="prop">
                                    {{ value }}
                                </td>
                            </tr>
                        </template>
                    </tbody>
                </table>

                <template v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')">
                    <h2>
                        Node Affinity:<br/>
                        <span class="normal">Required during scheduling ignored during execution </span>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution')"></span>
                    </h2>

                    <table class="clusterConfig">
                        <thead>
                            <th>Term <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items')"></span></th>
                            <th>Match <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions') + ' It can be either of type Expressions or Fields.'"></span></th>
                            <th>Requirement <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms.items.properties.matchExpressions.items')"></span></th>
                        </thead>
                        <tbody>
                            <template v-for="(term, i) in cluster.data.spec.pods.scheduling.nodeAffinity.requiredDuringSchedulingIgnoredDuringExecution.nodeSelectorTerms">
                                <tr>
                                    <td :rowspan="(term.hasOwnProperty('matchExpressions') && term.matchExpressions.length) + (term.hasOwnProperty('matchFields') && term.matchFields.length) + 1">
                                        Term #{{ i + 1 }} 
                                    </td>
                                    <td :rowspan="(term.hasOwnProperty('matchExpressions') ? term.matchExpressions.length : (term.hasOwnProperty('matchFields') && term.matchFields.length))">
                                        {{ term.hasOwnProperty('matchExpressions') ? 'Expressions' : 'Fields' }}
                                    </td>
                                    <td>
                                        <template v-if="term.hasOwnProperty('matchExpressions')">
                                            <strong>{{ term.matchExpressions[0].key }}</strong> <em>{{ affinityOperator(term.matchExpressions[0].operator) }}</em> <strong>{{ term.matchExpressions[0].hasOwnProperty('values') ? term.matchExpressions[0].values.join(', ') : ''}}</strong>	
                                        </template>
                                        <template v-else>
                                            <strong>{{ term.matchFields[0].key }}</strong> <em>{{ affinityOperator(term.matchFields[0].operator) }}</em> <strong>{{ term.matchFields[0].hasOwnProperty('values') ? term.matchFields[0].values.join(', ') : ''}}</strong>	
                                        </template>
                                    </td>
                                </tr>
                                <tr v-for="(exp, j) in term.matchExpressions" v-if="j > 0">
                                    <td>
                                        <strong>{{ exp.key }}</strong> <em>{{ affinityOperator(exp.operator) }}</em> <strong>{{ exp.hasOwnProperty('values') ? exp.values.join(', ') : ''}}</strong>
                                    </td>
                                </tr>
                                <tr v-for="(field, j) in term.matchFields">
                                    <td v-if="term.hasOwnProperty('matchExpressions') && !j" :rowspan="term.matchFields.length">
                                        Fields
                                    </td>
                                    <td>
                                        <strong>{{ field.key }}</strong> <em>{{ affinityOperator(field.operator) }}</em> <strong>{{ field.hasOwnProperty('values') ? field.values.join(', ') : ''}}</strong>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </template>

                <template v-if="hasProp(cluster, 'data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')">
                    <h2>
                        Node Affinity:<br/>
                        <span class="normal">Preferred during scheduling ignored during execution </span>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution')"></span>
                    </h2>

                    <table class="clusterConfig">
                        <thead>
                            <th>Term <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items')"></span></th>
                            <th>Weight <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.weight')"></span></th>
                            <th>Match <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions') + ' It can be either of type Expressions or Fields.'"></span></th>
                            <th>Requirement <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution.items.properties.preference.properties.matchExpressions.items')"></span></th>
                        </thead>
                        <tbody>
                            <template v-for="(term, i) in cluster.data.spec.pods.scheduling.nodeAffinity.preferredDuringSchedulingIgnoredDuringExecution">
                                <tr>
                                    <td :rowspan="(term.preference.hasOwnProperty('matchExpressions') && term.preference.matchExpressions.length) + (term.preference.hasOwnProperty('matchFields') && term.preference.matchFields.length)">
                                        Term #{{ i + 1 }}
                                    </td>
                                    <td :rowspan="(term.preference.hasOwnProperty('matchExpressions') && term.preference.matchExpressions.length) + (term.preference.hasOwnProperty('matchFields') && term.preference.matchFields.length)">
                                        {{ term.weight }}
                                    </td>
                                    <td :rowspan="(term.preference.hasOwnProperty('matchExpressions') ? term.preference.matchExpressions.length : (term.preference.hasOwnProperty('matchFields') && term.preference.matchFields.length))">
                                        {{ term.preference.hasOwnProperty('matchExpressions') ? 'Expressions' : 'Fields' }}
                                    </td>
                                    <td>
                                        <template v-if="term.preference.hasOwnProperty('matchExpressions')">
                                            <strong>{{ term.preference.matchExpressions[0].key }}</strong> <em>{{ affinityOperator(term.preference.matchExpressions[0].operator) }}</em> <strong>{{ term.preference.matchExpressions[0].hasOwnProperty('values') ? term.preference.matchExpressions[0].values.join(', ') : ''}}</strong>	
                                        </template>
                                        <template v-else>
                                            <strong>{{ term.preference.matchFields[0].key }}</strong> <em>{{ affinityOperator(term.preference.matchFields[0].operator) }}</em> <strong>{{ term.preference.matchFields[0].hasOwnProperty('values') ? term.preference.matchFields[0].values.join(', ') : ''}}</strong>	
                                        </template>
                                    </td>
                                </tr>
                                <tr v-for="(exp, j) in term.preference.matchExpressions" v-if="j > 0">
                                    <td>
                                        <strong>{{ exp.key }}</strong> <em>{{ affinityOperator(exp.operator) }}</em> <strong>{{ exp.hasOwnProperty('values') ? exp.values.join(', ') : ''}}</strong>
                                    </td>
                                </tr>
                                <tr v-for="(field, j) in term.preference.matchFields">
                                    <td v-if="term.preference.hasOwnProperty('matchExpressions') && !j" :rowspan="term.preference.matchFields.length">
                                        Fields
                                    </td>
                                    <td>
                                        <strong>{{ field.key }}</strong> <em>{{ affinityOperator(field.operator) }}</em> <strong>{{ field.hasOwnProperty('values') ? field.values.join(', ') : ''}}</strong>
                                    </td>
                                </tr>
                            </template>
                        </tbody>
                    </table>
                </template>
            </div>

            <div class="scripts" v-if="hasProp(cluster, 'data.spec.initialData.scripts')">
                <h2>Scripts <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.initialData.scripts')"></span></h2>
                <table class="clusterConfig">
                    <tbody>
                        <template v-for="(item, index) in cluster.data.spec.initialData.scripts">
                            <template v-if="hasProp(item, 'database')">
                                <tr>
                                    <td class="label" rowspan="2">
                                        Script #{{ index+1 }} <template v-if="hasProp(item, 'name')">– {{ item.name }} </template>
                                        <template v-if="hasProp(item, 'scriptFrom.secretKeyRef')">
                                            <br><span class="normal small">
                                                This script has been set from a SecretKeySelector
                                            </span>
                                        </template>
                                    </td>
                                    <td class="label">
                                        Database
                                    </td>
                                    <td colspan="2">
                                        {{ item.database }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Script Details 
                                    </td>
                                    <td colspan="2">
                                        <template v-if="hasProp(item, 'scriptFrom.secretKeyRef')">
                                            <a @click="setContentTooltip('#script-'+index)"> 
                                                View Details
                                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                            </a>
                                            <div :id="'script-'+index" class="hidden">
                                                <strong>Name</strong>: {{  item.scriptFrom.secretKeyRef.name }}<br/><br/>
                                                <strong>Key</strong>: {{  item.scriptFrom.secretKeyRef.key }}
                                            </div>
                                        </template>
                                        <template v-else>
                                            <a @click="setContentTooltip('#script-'+index)"> 
                                                View Script
                                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                            </a>
                                            <div :id="'script-'+index" class="hidden">
                                                <pre v-if="item.hasOwnProperty('script')">{{ item.script }}</pre>
                                                <pre v-else-if="hasProp(item, 'scriptFrom.configMapScript')">{{ item.scriptFrom.configMapScript }}</pre>
                                            </div>
                                        </template>
                                    </td>
                                </tr>
                            </template>		
                            <template v-else>
                                <tr>
                                    <td class="label">
                                        Script #{{ index+1 }} <template v-if="hasProp(item, 'name')">– {{ item.name }} </template>
                                        <template v-if="hasProp(item, 'scriptFrom.secretKeyRef')">
                                            <br><span class="normal small">
                                                This script has been set from a SecretKeySelector
                                            </span>
                                        </template>
                                    </td>
                                    <td class="label">
                                        Script Details
                                    </td>
                                    <td colspan="2">
                                        <template v-if="hasProp(item, 'scriptFrom.secretKeyRef')">
                                            <a @click="setContentTooltip('#script-'+index)"> 
                                                View Details
                                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                            </a>
                                            <div :id="'script-'+index" class="hidden">
                                                <strong>Name</strong>: {{  item.scriptFrom.secretKeyRef.name }}<br/><br/>
                                                <strong>Key</strong>: {{  item.scriptFrom.secretKeyRef.key }}
                                            </div>
                                        </template>
                                        <template v-else>
                                            <a @click="setContentTooltip('#script-'+index)"> 
                                                View Script
                                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                            </a>
                                            <div :id="'script-'+index" class="hidden">
                                                <pre v-if="item.hasOwnProperty('script')">{{ item.script }}</pre>
                                                <pre v-else-if="hasProp(item, 'scriptFrom.configMapScript')">{{ item.scriptFrom.configMapScript }}</pre>
                                            </div>
                                        </template>
                                    </td>
                                </tr>
                            </template>
                        </template>
                    </tbody>
                </table>
            </div>

            <div class="resourcesMetadata" v-if="hasProp(cluster, 'data.spec.metadata.annotations') && Object.keys(cluster.data.spec.metadata.annotations).length">
                <h2>Resources Annotations <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations')"></span></h2>
                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')" class="clusterConfig">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.allResources)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.allResources).length">
                                All Resources
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.allResources')"></span>
                            </td>
                            <td class="label">
                                {{ item.annotation }}
                            </td>
                            <td colspan="2">
                                {{ item.value }}
                            </td>
                        </tr>
                    </tbody>
                </table>

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.clusterPods')" class="clusterConfig">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.clusterPods)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.clusterPods).length">
                                Cluster Pods
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.clusterPods')"></span>
                            </td>
                            <td class="label">
                                {{ item.annotation }}
                            </td>
                            <td colspan="2">
                                {{ item.value }}
                            </td>
                        </tr>
                    </tbody>
                </table>

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')" class="clusterConfig">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.services)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.services).length">
                                Services
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.services')"></span>
                            </td>
                            <td class="label">
                                {{ item.annotation }}
                            </td>
                            <td colspan="2">
                                {{ item.value }}
                            </td>
                        </tr>
                    </tbody>
                </table>

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.primaryService')" class="clusterConfig">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.primaryService)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.primaryService).length">
                                Primary Service
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.primaryService')"></span>
                            </td>
                            <td class="label">
                                {{ item.annotation }}
                            </td>
                            <td colspan="2">
                                {{ item.value }}
                            </td>
                        </tr>
                    </tbody>
                </table>

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.replicasService')" class="clusterConfig">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.replicasService)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.replicasService).length">
                                Replicas Service
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.replicasService')"></span>
                            </td>
                            <td class="label">
                                {{ item.annotation }}
                            </td>
                            <td colspan="2">
                                {{ item.value }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="metadataLabels" v-if="hasProp(cluster, 'data.spec.metadata.labels')">
                <h2>Resources Labels <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels')"></span></h2>
                <table v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods')" class="clusterConfig">
                    <tbody>
                        <tr v-for="(value, label, index) in cluster.data.spec.metadata.labels.clusterPods">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.labels.clusterPods).length">
                                Cluster Pods
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.labels.clusterPods')"></span>
                            </td>
                            <td class="label">
                                {{ label }}
                            </td>
                            <td colspan="2">
                                {{ value }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <div class="postgresServices" v-if="hasProp(cluster, 'data.spec.postgresServices') && ((hasProp(cluster, 'data.spec.postgresServices.primary') && cluster.data.spec.postgresServices.primary.enabled) || (hasProp(cluster, 'data.spec.postgresServices.replicas') && cluster.data.spec.postgresServices.replicas.enabled))">
                <h2>Postgres Services <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresServices')"></span></h2>

                <table v-for="(service, serviceName) in cluster.data.spec.postgresServices" class="clusterConfig">
                    <tbody>
                        <tr>
                            <td class="label capitalize" rowspan="2">
                                {{ serviceName }}
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName)"></span>
                            </td>
                            <td class="label">
                                Name
                            </td>
                            <td colspan="2">
                                {{ cluster.data.metadata.name }}-{{serviceName}}.{{cluster.data.metadata.namespace}}
                            </td>	
                        </tr>
                        <tr>
                            <td class="label">
                                Type
                                <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName+'.type')"></span>
                            </td>
                            <td colspan="2">
                                {{ service.type }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            
            <div class="postgresExtensions" v-if="hasProp(cluster, 'data.spec.postgres.extensions') && cluster.data.spec.postgres.extensions.length">
                <h2>Postgres Extensions Deployed/To Be Deployed <span class="helpTooltip"  :data-tooltip="getTooltip('sgcluster.spec.postgres.extensions')"></span></h2>
                <span class="warning">The extension(s) are installed into the StackGres Postgres container. To start using them, you need to execute an appropriate <code>CREATE EXTENSION</code> command in the database(s) where you want to use the extension(s). Note that depending on each extension's requisites you may also need to add configuration to the cluster's <code>SGPostgresConfig</code> configuration, like adding the extension to <code>shared_preload_libraries</code> or adding extension-specific configuration parameters.</span>

                <table class="clusterConfig">
                    <thead>
                        <th>
                            Name
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.name')"></span>
                        </th>
                        <th class="textRight">
                            Version
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.versions')"></span>
                        </th>
                        <th colspan="2">
                            Description
                            <span class="helpTooltip"  :data-tooltip="getTooltip('sgextensions.extensions.description')"></span>
                        </th>
                    </thead>
                    <tbody>
                        <tr v-for="ext in sortExtensions(cluster.data.spec.postgres.extensions)">
                            <template v-for="extInfo in extensionsList" v-if="ext.name == extInfo.name">
                                <td class="label">
                                    <a v-if="extInfo.hasOwnProperty('url') && extInfo.url" :href="extInfo.url" target="_blank" class="newTab" :title="extInfo.url">
                                        {{ ext.name }}
                                    </a>
                                    <template v-else>
                                        {{ ext.name }}
                                    </template>
                                </td>
                                <td class="textRight">
                                    {{ ext.version }}
                                </td>
                                <td class="firstLetter" colspan="2">
                                    {{ extInfo.abstract }}
                                </td>
                            </template>
                        </tr>
                    </tbody>
                </table>
            </div>
        </template>
    </div>
</template>

<script>
	import store from '../../store'
	import { mixin } from '../mixins/mixin'

    export default {
        name: 'ClusterDetails',

		mixins: [mixin],

        props: ['cluster', 'extensionsList'],

		methods: {

			unparseProps ( props, key = 'annotation' ) {
				var propsArray = [];
				if(!$.isEmptyObject(props)) {
					Object.entries(props).forEach(([k, v]) => {
						var prop = {};
						prop[key] = k;
						prop['value'] = v;
						propsArray.push(prop)
					});
				}
				
				return propsArray
			},

			countObjectArrayKeys(objectArray) {
				let count = 0;

				objectArray.forEach(function(obj, index) {
					count += Object.keys(obj).length
				})

				return count
			},

			sortExtensions(ext) {
				return [...ext].sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
			},

			clusterProfileMismatch(cluster, profile) {
				const vc = this;
				
				if(vc.hasProp(cluster, 'status.pods') && cluster.status.pods.length) {
					let mismatch = cluster.status.pods.find(pod => (( pod.cpuRequested != ( (pod.cpuRequested.includes('m') && !profile.data.spec.cpu.includes('m')) ? ( (profile.data.spec.cpu * 1000) + 'm' ) : profile.data.spec.cpu ) ) || (pod.memoryRequested.replace('.00','') != profile.data.spec.memory)))				
					return (typeof mismatch !== 'undefined')
				} else {
					return false;
				}

			},

			affinityOperator(op) {

				switch(op) {

					case 'NotIn':
						op = 'not in';
						break;
					case 'DoesNotExists':
						op = 'does not exists';
						break;
					case 'Gt':
						op = 'greather than';
						break;
					case 'Lt':
						op = 'less than';
						break;
				}

				return op.toLowerCase();

			}

		},

		computed: {

			tooltips () {
				return store.state.tooltips
			},

			profiles () {
				return store.state.sginstanceprofiles
			},

			backups () {
				return store.state.sgbackups
			}
		},

	}
</script>

<style scoped>
	.clusterConfig td {
		position: relative;
	}

	.helpTooltip.alert {
		position: absolute;
		right: 30px;
		top: 11px;
	}

	.postgresExtensions th {
		padding-left: 10px;
	}

	a.newTab {
    	background: none !important;
		color: var(--textColor);
	}

	a.newTab:after {
		position: relative;
		display: inline-block;
		content: "";
		height: 12px;
		width: 12px;
		left: 5px;
    	background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+PGcgZmlsbD0iIzE3MTcxNyI+PHBhdGggZD0iTS4zMDEgMTQuN2MtLjItLjItLjMtLjQtLjMtLjdWMy4yYzAtLjYuNS0xIDEtMWg1LjRjLjYgMCAxIC41IDEgMXMtLjUgMS0xIDFoLTQuNFYxM2g4LjhWOC42YzAtLjYuNS0xIDEtMXMxIC41IDEgMVYxNGMwIC42LS41IDEtMSAxaC0xMC44Yy0uMyAwLS41LS4xLS43LS4zeiIvPjxwYXRoIGQ9Ik0xNS4wMDEgNi40VjFjMC0uNi0uNS0xLTEtMWgtNS40Yy0uNiAwLTEgLjQtMSAxcy40IDEgMSAxaDIuOWwtNS4xIDUuMWMtLjQuNC0uNCAxIDAgMS40LjQuNCAxIC40IDEuNCAwbDUuMi01djIuOWMwIC42LjQgMSAxIDEgLjUuMSAxLS40IDEtMXoiLz48L2c+PC9zdmc+)  no-repeat !important;
	}

	.darkmode a.newTab:after {
		background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+PGcgZmlsbD0iI2ZmZmZmZiI+PHBhdGggZD0iTS4zMDEgMTQuN2MtLjItLjItLjMtLjQtLjMtLjdWMy4yYzAtLjYuNS0xIDEtMWg1LjRjLjYgMCAxIC41IDEgMXMtLjUgMS0xIDFoLTQuNFYxM2g4LjhWOC42YzAtLjYuNS0xIDEtMXMxIC41IDEgMVYxNGMwIC42LS41IDEtMSAxaC0xMC44Yy0uMyAwLS41LS4xLS43LS4zeiIvPjxwYXRoIGQ9Ik0xNS4wMDEgNi40VjFjMC0uNi0uNS0xLTEtMWgtNS40Yy0uNiAwLTEgLjQtMSAxcy40IDEgMSAxaDIuOWwtNS4xIDUuMWMtLjQuNC0uNCAxIDAgMS40LjQuNCAxIC40IDEuNCAwbDUuMi01djIuOWMwIC42LjQgMSAxIDEgLjUuMSAxLS40IDEtMXoiLz48L2c+PC9zdmc+)  no-repeat !important;	
	}

	a.newTab:hover:after {
		background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0iTS4zMDEgMTQuN2MtLjItLjItLjMtLjQtLjMtLjdWMy4yYzAtLjYuNS0xIDEtMWg1LjRjLjYgMCAxIC41IDEgMXMtLjUgMS0xIDFoLTQuNFYxM2g4LjhWOC42YzAtLjYuNS0xIDEtMXMxIC41IDEgMVYxNGMwIC42LS41IDEtMSAxaC0xMC44Yy0uMyAwLS41LS4xLS43LS4zeiIvPjxwYXRoIGQ9Ik0xNS4wMDEgNi40VjFjMC0uNi0uNS0xLTEtMWgtNS40Yy0uNiAwLTEgLjQtMSAxcy40IDEgMSAxaDIuOWwtNS4xIDUuMWMtLjQuNC0uNCAxIDAgMS40LjQuNCAxIC40IDEuNCAwbDUuMi01djIuOWMwIC42LjQgMSAxIDEgLjUuMSAxLS40IDEtMXoiLz48L2c+PC9zdmc+)  no-repeat !important;
	}

	.warning {
		background: rgba(0,173,181,.05);
		border: 1px solid var(--blue);
		padding: 20px;
		border-radius: 6px;
		display: inline-block;
		line-height: 1.1;
		width: 100%;
	}

	.trimText {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		display: block;
		max-width: 250px;
		width: 100%;
	}

	th.textRight > span {
    	margin-right: 10px;
	}
</style>
