<template>
	<div id="clusterDetails">
        <template v-if="cluster.hasOwnProperty('data')">
            <table class="crdDetails">
                <tbody>
                    <tr>
                        <td class="label">
                            Cluster Namespace
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.namespace')"></span>
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
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.version')"></span>
                        </td>
                        <td colspan="3">{{ cluster.data.spec.postgres.version }}</td>
                    </tr>
                    <template v-if="hasProp(cluster,'data.spec.postgres.ssl.enabled') && cluster.data.spec.postgres.ssl.enabled">
                        <tr>
                            <td class="label" :rowspan="4">
                                SSL Connections
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl')"></span>
                            </td>
                            <td class="label" :rowspan="2">
                                Certificate Secret Key Selector
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector')"></span>
                            </td>
                            <td class="label">
                                Secret Name
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector.name')"></span>
                            </td>
                            <td>
                                {{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.name }}
                            </td>
                        </tr>
                        <tr>
                            <td class="label">
                                Secret Key
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.certificateSecretKeySelector.key')"></span>
                            </td>
                            <td>
                                {{ cluster.data.spec.postgres.ssl.certificateSecretKeySelector.key }}
                            </td>
                        </tr>
                        <tr>
                            <td class="label" :rowspan="2">
                                Private Key Secret Key Selector
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector')"></span>
                            </td>
                            <td class="label">
                                Secret Name
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.name')"></span>
                            </td>
                            <td>
                                {{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.name }}
                            </td>
                        </tr>
                        <tr>
                            <td class="label">
                                Secret Key
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.ssl.privateKeySecretKeySelector.key')"></span>
                            </td>
                            <td>
                                {{ cluster.data.spec.postgres.ssl.privateKeySecretKeySelector.key }}
                            </td>
                        </tr>
                    </template>
                    <tr>
                        <td class="label">
                            Instances
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
                        </td>
                        <td colspan="3" class="textRight">{{ cluster.data.spec.instances }}</td>
                    </tr>
                    <tr>
                        <td class="label">
                            Instance Profile
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.sgInstanceProfile')"></span>
                        </td>
                        <template v-if="hasProp(cluster, 'data.spec.sgInstanceProfile')">
                            <td colspan="3" v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
                                <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + cluster.data.spec.sgInstanceProfile">
                                    {{ cluster.data.spec.sgInstanceProfile }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }}) 
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                </router-link>
                                <!--Temporarily disabled until REST API response data is set properly-->
                                
                                <!--<template v-if="clusterProfileMismatch(cluster, profile)">
                                    <span class="helpTooltip alert" data-tooltip="This profile has been modified recently. Cluster must be restarted in order to apply such changes."></span>
                                </template>-->
                            </td>
                        </template>
                        <template v-else>
                            <td colspan="3">
                                Default (Cores: 1, RAM: 2GiB)
                            </td>
                        </template>
                    </tr>
                    <tr>
                        <td class="label" rowspan="4">
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
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.storageClass')"></span>
                        </td>
                        <td>{{ cluster.data.spec.pods.persistentVolume.storageClass }}</td>
                    </tr>
                    <tr>
                        <td class="label">
                            Connection Pooling
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                        </td>
                        <td colspan="2">
                            {{ hasProp(cluster, 'data.spec.pods.disableConnectionPooling') ? isEnabled(cluster.data.spec.pods.disableConnectionPooling, true) : 'Enabled' }}
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Postgres Utils
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disablePostgresUtil').replace('If set to `true`', 'If disabled')"></span>
                        </td>
                        <td colspan="2">
                            {{ isEnabled(cluster.data.spec.pods.disablePostgresUtil, true) }}
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Metrics Exporter
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.disableMetricsExporter').replace('If set to `true`', 'If disabled').replace('Recommended', 'Recommended to be disabled')"></span>
                        </td>
                        <td colspan="3">
                            {{ isEnabled(cluster.data.spec.pods.disableMetricsExporter, true )}}
                        </td>
                    </tr>
                    <template v-if="hasProp(cluster, 'data.spec.replicateFrom')">
                        <tr>
                            <td class="label" :rowspan="getReplicationRows(cluster.data.spec.replicateFrom)">
                                Replicate From
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom')"></span>
                            </td>
                            <td class="label">
                                Source
                                <span class="helpTooltip" data-tooltip="Specifies the source from which this cluster is being replicated."></span>
                            </td>
                            <td colspan="2">
                                <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.sgCluster')">
                                    Local Cluster
                                </template>
                                <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external')">
                                    External Instance
                                </template>
                                <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external') && hasProp(cluster, 'data.spec.replicateFrom.storage')">
                                     + 
                                </template>
                                <template v-if="hasProp(cluster, 'data.spec.replicateFrom.storage')">
                                    Object Storage
                                </template>
                            </td>
                        </tr>
                        <tr v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.sgCluster')">
                            <td class="label">
                                Cluster
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.sgCluster')"></span>
                            </td>
                            <td colspan="2">
                                <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.data.spec.replicateFrom.instance.sgCluster" target="_blank">
                                    {{ cluster.data.spec.replicateFrom.instance.sgCluster }}
                                    <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                </router-link>
                            </td>
                        </tr>
                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.instance.external')">
                            <tr>
                                <td class="label">
                                    Host
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external.host')"></span>
                                </td>
                                <td colspan="2">
                                   {{ cluster.data.spec.replicateFrom.instance.external.host }}
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Port
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.instance.external.port')"></span>
                                </td>
                                <td colspan="2">
                                   {{ cluster.data.spec.replicateFrom.instance.external.port }}
                                </td>
                            </tr>
                        </template>
                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.storage')">
                            <tr>
                                <td class="label">
                                    Object Storage
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.sgObjectStorage')"></span>
                                </td>
                                <td colspan="2">
                                    <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + cluster.data.spec.replicateFrom.storage.sgObjectStorage" target="_blank">
                                        {{ cluster.data.spec.replicateFrom.storage.sgObjectStorage }}
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                    </router-link>
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Path
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.path')"></span>
                                </td>
                                <td colspan="2">
                                   {{ cluster.data.spec.replicateFrom.storage.path }}
                                </td>
                            </tr>
                            <template v-if="cluster.data.spec.replicateFrom.storage.hasOwnProperty('performance')">
                                <tr v-for="(val, key, index) in cluster.data.spec.replicateFrom.storage.performance">
                                    <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.replicateFrom.storage.performance).length">
                                        Performance
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance')"></span>
                                    </td>
                                    <td class="label capitalize">
                                        {{ splitUppercase(key) }}
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.storage.performance.' + key)"></span>
                                    </td>
                                    <td>
                                        {{ val }}
                                    </td>
                                </tr>
                            </template>
                        </template>
                        <template v-if="hasProp(cluster, 'data.spec.replicateFrom.users')">
                            <tr>
                                <td class="label" :rowspan="4">
                                    Superuser Credentials
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser')"></span>
                                </td>
                                <td class="label">
                                    Username Secret Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username.name')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.superuser.username.name }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Username Secret Key
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.username.key')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.superuser.username.key }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Password Secret Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password.name')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.superuser.password.name }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Password Secret Key
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.superuser.password.key')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.superuser.password.key }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label" :rowspan="4">
                                    Replication User Credentials
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication')"></span>
                                </td>
                                <td class="label">
                                    Username Secret Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username.name')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.replication.username.name }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Username Secret Key
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.username.key')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.replication.username.key }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Password Secret Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password.name')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.replication.password.name }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Password Secret Key
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.replication.password.key')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.replication.password.key }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label" :rowspan="4">
                                    Authenticator User Credentials
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator')"></span>
                                </td>
                                <td class="label">
                                    Username Secret Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username.name')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.authenticator.username.name }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Username Secret Key
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.username.key')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.authenticator.username.key }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Password Secret Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password.name')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.authenticator.password.name }} 
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Password Secret Key
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.replicateFrom.users.authenticator.password.key')"></span>
                                </td>
                                <td>
                                    {{ cluster.data.spec.replicateFrom.users.authenticator.password.key }} 
                                </td>
                            </tr>
                        </template>
                    </template>
                    <tr>
                        <td class="label" :rowspan="( Object.keys(cluster.data.spec.replication).length + (cluster.data.spec.replication.hasOwnProperty('groups') && (cluster.data.spec.replication.groups.length - 1) ) )">
                            Pods Replication
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
                            {{ (typeof cluster.data.spec.prometheusAutobind !== 'undefined') ? isEnabled(cluster.data.spec.pods.disableMetricsExporter, true) : 'Disabled' }}
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Prometheus Autobind
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.prometheusAutobind')"></span>
                        </td>
                        <td colspan="3">
                            <template v-if="(typeof cluster.data.spec.prometheusAutobind !== 'undefined')">
                                Enabled
                            </template>
                            <template v-else>
                                Disabled
                            </template>
                        </td>
                    </tr>
                    <tr>
                        <td class="label">
                            Non-Production Settings
                        </td>
                        <td class="label">
                            Cluster Pod Anti Affinity
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('Set this property to true to allow','When disabled, it allows running')"></span>
                        </td>
                        <td colspan="2">
                            <template v-if="(typeof cluster.data.spec.nonProductionOptions !== 'undefined')">
                                {{ cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity ? 'Disabled' : 'Enabled' }}
                            </template>
                            <template v-else>
                                Enabled
                            </template>
                        </td>
                    </tr>
                    <tr v-if="hasProp(cluster, 'data.spec.distributedLogs.sgDistributedLogs')">
                        <td class="label" :rowspan="Object.keys(cluster.data.spec.distributedLogs).length">
                            Distributed Logs
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs')"></span>
                        </td>
                        <td class="label">
                            Logs Server
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.sgDistributedLogs')"></span>
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
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.distributedLogs.retention')"></span>
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
                <table class="crdDetails">
                    <tbody>
                        <tr>
                            <td class="label">
                                Postgres
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPostgresConfig')"></span>
                            </td>
                            <template v-if="hasProp(cluster, 'data.spec.configurations.sgPostgresConfig')">
                                <td colspan="3">
                                    <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + cluster.data.spec.configurations.sgPostgresConfig">
                                        {{ cluster.data.spec.configurations.sgPostgresConfig }}
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                    </router-link>								
                                </td>
                            </template>
                            <template v-else>
                                <td colspan="3">
                                    Default
                                </td>
                            </template>
                        </tr>
                        <tr v-if="!cluster.data.spec.pods.disableConnectionPooling">
                            <td class="label">
                                Connection Pooling
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.sgPoolingConfig')"></span>
                            </td>
                            <template v-if="hasProp(cluster, 'data.spec.configurations.sgPoolingConfig')">
                                <td colspan="3">
                                    <router-link :to="'/' + $route.params.namespace + '/sgpoolconfig/' + cluster.data.spec.configurations.sgPoolingConfig">
                                        {{ cluster.data.spec.configurations.sgPoolingConfig }}
                                        <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                    </router-link>	
                                </td>
                            </template>
                            <template v-else-if="!hasProp(cluster, 'data.spec.pods.disableConnectionPooling') || !cluster.data.spec.pods.disableConnectionPooling">
                                <td colspan="3">
                                    Default
                                </td>
                            </template>
                        </tr>
                        <template v-if="hasProp(cluster, 'data.spec.configurations.backups')">
                            <template v-for="backup in cluster.data.spec.configurations.backups">
                                <tr>
                                    <td class="label" :rowspan="(Object.keys(backup).length + ( hasProp(backup, 'performance.uploadDiskConcurrency') ? 3 : 2 ) )">
                                        Managed Backups
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups')"></span>
                                    </td>
                                    <td class="label">
                                        Object Storage
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.sgObjectStorage')"></span>
                                    <td :colspan="backup.hasOwnProperty('performance') && 2">
                                        <router-link :to="'/' + $route.params.namespace + '/sgobjectstorage/' + backup.sgObjectStorage">
                                            {{ backup.sgObjectStorage }}
                                            <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                        </router-link>
                                    </td>
                                </tr>
                                <tr v-if="backup.hasOwnProperty('cronSchedule')">
                                    <td class="label">
                                        Cron Schedule
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.cronSchedule')"></span>
                                    </td>
                                    <td :colspan="backup.hasOwnProperty('performance') && 2">
                                        {{ tzCrontab(backup.cronSchedule) | prettyCRON(false) }}
                                    </td>
                                </tr>
                                <tr v-if="(backup.hasOwnProperty('path') && backup.path.length)">
                                    <td class="label">
                                        Path
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.path')"></span>
                                    </td>
                                    <td :colspan="backup.hasOwnProperty('performance') && 2">
                                        {{ backup.path }}
                                    </td>
                                </tr>
                                <tr v-if="backup.hasOwnProperty('retention')">
                                    <td class="label">
                                        Retention Window
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.retention')"></span>
                                    </td>
                                    <td :colspan="backup.hasOwnProperty('performance') && 2">
                                        {{ backup.retention }}
                                    </td>
                                </tr>
                                <tr v-if="backup.hasOwnProperty('retention')">
                                    <td class="label">
                                        Compression Method
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.compression')"></span>
                                    </td>
                                    <td :colspan="backup.hasOwnProperty('performance') && 2">
                                        {{ backup.compression }}
                                    </td>
                                </tr>
                                <tr v-if="backup.hasOwnProperty('performance')">
                                    <td class="label" :rowspan="(backup.performance.hasOwnProperty('uploadDiskConcurrency') ? 3 : 2)">
                                        Performance
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance')"></span>
                                    </td>
                                    <td class="label">
                                        Maximum Network Bandwidth
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.maxNetworkBandwidth')"></span>
                                    </td>
                                    <td>
                                        {{ backup.performance.hasOwnProperty('maxNetworkBandwidth') ? backup.performance.maxNetworkBandwidth : 'Unlimited' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Maximum Disk Bandwidth
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.maxDiskBandwidth')"></span>
                                    </td>
                                    <td>
                                        {{ backup.performance.hasOwnProperty('maxDiskBandwidth') ? backup.performance.maxDiskBandwidth : 'Unlimited' }}
                                    </td>
                                </tr>
                                <tr v-if="backup.performance.hasOwnProperty('uploadDiskConcurrency')">
                                    <td class="label">
                                        Upload Disk Concurrency
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.configurations.backups.performance.properties.uploadDiskConcurrency')"></span>
                                    </td>
                                    <td>
                                        {{ backup.performance.uploadDiskConcurrency }}
                                    </td>
                                </tr>
                            </template>
                        </template>
                    </tbody>
                </table>
            </div>

            <div class="podsScheduling" v-if="hasProp(cluster, 'data.spec.pods.scheduling')">
                <h2>Pods Scheduling <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.scheduling')"></span></h2>
                <table class="crdDetails">
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
                                <td class="label capitalize">
                                    {{ prop == 'tolerationSeconds' ? 'Toleration Seconds' : prop }}
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

                    <table class="crdDetails">
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

                    <table class="crdDetails">
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

            <div class="scripts" v-if="hasProp(cluster, 'data.spec.managedSql')">
                <h2>Managed SQL <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql')"></span></h2>
                
                <div class="configurationDetails">
                    <table class="crdDetails">
                        <tbody>
                            <template v-for="(baseScript, baseIndex) in cluster.data.spec.managedSql.scripts">
								<tr>
                                    <td rowspan="4">
                                        Script #{{ baseIndex + 1 }}
                                    </td>
									<td class="label">
                                        Name 
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.metadata.name')"></span>
                                    </td>
									<td>
                                        <template v-if="scripts.find(s => (s.name == baseScript.sgScript))">
                                            <router-link :to="'/' + cluster.data.metadata.namespace + '/sgscript/' + baseScript.sgScript">
                                                {{ baseScript.sgScript }}
                                                <svg data-v-7f311205="" xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g data-v-7f311205="" transform="translate(0 -126.766)"><path data-v-7f311205="" d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"></path><path data-v-7f311205="" d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"></path></g></svg>
                                            </router-link>
                                        </template>

                                        <template v-else>
                                            {{ baseScript.sgScript }}
                                        </template>
                                    </td>
								</tr>

                                <template v-if="baseScript.hasOwnProperty('scriptSpec')">
                                    <tr>
                                        <td class="label">
                                            Continue on Error 
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.continueOnError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                        </td>
                                        <td>
                                            {{ baseScript.scriptSpec.hasOwnProperty('continueOnError') ? isEnabled(baseScript.scriptSpec.continueOnError) : 'Disabled' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Managed Versions 
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.managedVersions').replace(/true/g, 'Enabled')"></span>
                                        </td>
                                        <td>
                                            {{ baseScript.scriptSpec.hasOwnProperty('managedVersions') ? isEnabled(baseScript.scriptSpec.managedVersions) : 'Enabled' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Entries
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.script').replace(/ This field is mutually exclusive with `scriptFrom` field./g, '').replace('script','scripts')"></span>
                                        </td>
                                        <td>
                                            <a @click="setContentTooltip('#baseScript-' + baseIndex)"> 
                                                View Entries ({{ baseScript.scriptSpec.scripts.length }})
                                                <svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
                                            </a>
                                        </td>
                                    </tr>
                                </template>

                                <div class="configurationDetails hidden" :id="'baseScript-' + baseIndex" v-if="baseScript.hasOwnProperty('scriptSpec')">
                                    <h2>Script Entries <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts')"></span></h2>

                                    <table class="crdDetails" v-for="(script, index) in baseScript.scriptSpec.scripts">
                                        <tbody>
                                            <template>
                                                <tr>
                                                    <td :rowspan="999">
                                                        Entry #{{ index + 1 }}
                                                    </td>
                                                    <td class="label">
                                                        ID
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.id')"></span>
                                                    </td>
                                                    <td class="textRight">
                                                        {{ script.id }}
                                                    </td>
                                                </tr>
                                                <tr v-if="script.hasOwnProperty('name')">
                                                    <td class="label">
                                                        Name
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.name')"></span>
                                                    </td>
                                                    <td>
                                                        {{ script.name }}
                                                    </td>
                                                </tr>
                                                <tr v-if="script.hasOwnProperty('version')">
                                                    <td class="label">
                                                        Version
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.version')"></span>
                                                    </td>
                                                    <td class="textRight">
                                                        {{ script.version }}
                                                    </td>
                                                </tr>
                                                <tr v-if="script.hasOwnProperty('database')">
                                                    <td class="label">
                                                        Database
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.database')"></span>
                                                    </td>
                                                    <td>
                                                        {{ script.database }}
                                                    </td>
                                                </tr>
                                                <tr v-if="script.hasOwnProperty('user')">
                                                    <td class="label">
                                                        User
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.user')"></span>
                                                    </td>
                                                    <td>
                                                        {{ script.user }}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td class="label">
                                                        Retry on Error
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.retryOnError').replace(/false/g, 'Disabled').replace(/true/g, 'Disabled')"></span>
                                                    </td>
                                                    <td>
                                                        {{ script.hasOwnProperty('retryOnError') ? isEnabled(script.retryOnError) : 'Disabled' }}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td class="label">
                                                        Store Status in Database
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.storeStatusInDatabase').replace(/false/g, 'Disabled').replace(/true/g, 'Disabled')"></span>
                                                    </td>
                                                    <td>
                                                        {{ script.hasOwnProperty('storeStatusInDatabase') ? isEnabled(script.storeStatusInDatabase) : 'Disabled' }}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td class="label">
                                                        Wrap in Transaction
                                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgscript.spec.scripts.wrapInTransaction').replace('If not set', 'If Disabled')"></span>
                                                    </td>
                                                    <td class="upper">
                                                        {{ script.hasOwnProperty('wrapInTransaction') ? script.wrapInTransaction : 'Disabled' }}
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td class="label">
                                                        Script
                                                        <span class="helpTooltip" :data-tooltip="( script.hasOwnProperty('scriptFrom') ? getTooltip('sgscript.spec.scripts.scriptFrom') : getTooltip('sgscript.spec.scripts.script') )"></span>
                                                    </td>
                                                    <td>
                                                        <template v-if="hasProp(script, 'scriptFrom.secretKeyRef')">
                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)" :data-content-tooltip="('#script-' + baseIndex + '-' + index)" class="eye-icon">
                                                                View Key Reference
                                                            </a>
                                                            <div :id="'script-' + baseIndex + '-' + index" class="hidden">
                                                                <strong>Name</strong>: {{  script.scriptFrom.secretKeyRef.name }}<br/><br/>
                                                                <strong>Key</strong>: {{  script.scriptFrom.secretKeyRef.key }}
                                                            </div>
                                                        </template>
                                                        <template v-else>
                                                            <a @click="setContentTooltip('#script-' + baseIndex + '-' + index)" :data-content-tooltip="('#script-' + baseIndex + '-' + index)" class="eye-icon"> 
                                                                View Script
                                                            </a>
                                                            <div :id="'script-'+ baseIndex + '-' + index" class="hidden">
                                                                <pre v-if="script.hasOwnProperty('script')">{{ script.script }}</pre>
                                                                <pre v-else-if="hasProp(script, 'scriptFrom.configMapScript')">{{ script.scriptFrom.configMapScript }}</pre>
                                                            </div>
                                                        </template>
                                                    </td>
                                                </tr>
                                            </template>
                                        </tbody>
                                    </table>
                                </div>	
				            </template>
                            <tr>
                                <td class="label">
                                    Continue on SGScript Error   
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.managedSql.continueOnSGScriptError').replace(/true/g, 'Enabled').replace('false','Disabled')"></span>
                                </td>
                                <td colspan="2">
                                    {{ isEnabled(cluster.data.spec.managedSql.continueOnSGScriptError) }}
                                </td>
                            </tr>
						</tbody>
					</table>
				</div>
            </div>

            <div class="resourcesMetadata" v-if="hasProp(cluster, 'data.spec.metadata.annotations') && Object.keys(cluster.data.spec.metadata.annotations).length">
                <h2>Resources Annotations <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations')"></span></h2>
                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')" class="crdDetails">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.allResources)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.allResources).length">
                                All Resources
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.allResources')"></span>
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

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.clusterPods')" class="crdDetails">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.clusterPods)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.clusterPods).length">
                                Cluster Pods
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.clusterPods')"></span>
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

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')" class="crdDetails">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.services)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.services).length">
                                Services
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.services')"></span>
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

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.primaryService')" class="crdDetails">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.primaryService)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.primaryService).length">
                                Primary Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.primaryService')"></span>
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

                <table v-if="hasProp(cluster, 'data.spec.metadata.annotations.replicasService')" class="crdDetails">
                    <tbody>
                        <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.replicasService)">
                            <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.replicasService).length">
                                Replicas Service
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.metadata.annotations.replicasService')"></span>
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
                <table v-if="hasProp(cluster, 'data.spec.metadata.labels.clusterPods')" class="crdDetails">
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
                <h2>Postgres Services <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices')"></span></h2>

                <table v-for="(service, serviceName) in cluster.data.spec.postgresServices" class="crdDetails">
                    <tbody>
                        <tr>
                            <td class="label capitalize" rowspan="3">
                                {{ serviceName }}
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName)"></span>
                            </td>
                            <td class="label">
                                Status
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName+'.enabled')"></span>
                            </td>
                            <td colspan="2">
                                {{ isEnabled(service.enabled) }}
                            </td>
                        </tr>
                        <tr>
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
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgresServices.'+serviceName+'.type')"></span>
                            </td>
                            <td colspan="2">
                                {{ service.type }}
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            
            <div class="postgresExtensions" v-if="hasProp(cluster, 'data.spec.postgres.extensions') && cluster.data.spec.postgres.extensions.length">
                <h2>Postgres Extensions Deployed/To Be Deployed <span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.postgres.extensions')"></span></h2>
                <span class="warning">The extension(s) are installed into the StackGres Postgres container. To start using them, you need to execute an appropriate <code>CREATE EXTENSION</code> command in the database(s) where you want to use the extension(s). Note that depending on each extension's requisites you may also need to add configuration to the cluster's <code>SGPostgresConfig</code> configuration, like adding the extension to <code>shared_preload_libraries</code> or adding extension-specific configuration parameters.</span>

                <table class="crdDetails">
                    <thead>
                        <th>
                            Name
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.name')"></span>
                        </th>
                        <th class="textRight">
                            Version
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.versions')"></span>
                        </th>
                        <th colspan="2">
                            Description
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgextensions.extensions.description')"></span>
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
                                    <template v-if="(ext.name == 'timescaledb_tsl')">
                                        <hr>
                                        The license for this extension is not open source. Please check licensing details with the creators of the extension.
                                    </template>
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

			},

            getReplicationRows(rep) {
                const vc = this;
                
                if(vc.hasProp(rep, 'instance.sgCluster')) {
                    return 2
                } else {
                    let count = 13;

                    if (vc.hasProp(rep, 'instance.external')) {
                        count += 2;
                    }

                    if(rep.hasOwnProperty('storage')) {
                        count += 2;

                        if(rep.storage.hasOwnProperty('performance')) {
                            count += Object.keys(rep.storage.performance).length
                        }
                    }
                    
                    return count
                }
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
			},

            scripts () {
                return store.state.sgscripts
            }
		},

        mounted: function() {

            const vc = this;

            $(document).on('click', '[data-content-tooltip]', (el) => {
                vc.setContentTooltip(el.target.dataset.contentTooltip);
            })

        }

	}
</script>

<style scoped>
	.clusterConfig td {
		position: relative;
	}

	.helpTooltip.alert {
		position: absolute;
		right: 30px;
		top: 12px;
        left: auto;
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

    .eye-icon {
        padding-right: 29px;
    }

    .eye-icon:after {
        content: '';
        width: 18px;
        height: 14px;
        transform: scale(0.85);
        display: inline-block;
        position: absolute;
        right: 10px;
    }
</style>
