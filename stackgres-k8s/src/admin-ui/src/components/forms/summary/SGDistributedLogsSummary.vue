<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section"
            v-if="(showDefaults || (
                ((crd.data.spec.persistentVolume.size != '1Gi') || hasProp(crd, 'data.spec.persistentVolume.storageClass')) ||
                (hasProp(crd, 'data.spec.sgInstanceProfile') || hasProp(crd, 'data.spec.configurations.sgPostgresConfig'))
            )
        )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Specs </strong>
                <ul>                     
                    <li v-if="showDefaults || (crd.data.spec.persistentVolume.size != '1Gi') || hasProp(crd, 'data.spec.persistentVolume.storageClass')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Persistent Volume</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume')"></span>
                        <ul>
                            <li v-if="showDefaults || (crd.data.spec.persistentVolume.size != '1Gi')">
                                <strong class="label">Volume Size</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.size')"></span>
                                <span class="value"> : {{ crd.data.spec.persistentVolume.size }}B</span>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.persistentVolume.storageClass')">
                                <strong class="label">Storage Class</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.storageClass')"></span>
                                <span class="value"> : {{ crd.data.spec.persistentVolume.storageClass }}</span>
                            </li>
                        </ul>
                    </li>

                    <li v-if="showDefaults || hasProp(crd, 'data.spec.sgInstanceProfile') || hasProp(crd, 'data.spec.configurations.sgPostgresConfig')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Pods Resources</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.resources')"></span>
                        <ul>
                            <li v-if="showDefaults || hasProp(crd, 'data.spec.sgInstanceProfile')">
                                <strong class="label">Instance Profile</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.sgInstanceProfile')"></span>
                                <span class="value">
                                    <template v-if="hasProp(crd, 'data.spec.sgInstanceProfile')"> : 
                                        <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == crd.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == crd.data.metadata.namespace) )">
                                            {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </template>
                                    <template v-else> : Default (Cores: 1, RAM: 2GiB)</template>
                                </span>
                            </li>
                            <li v-if="showDefaults || hasProp(crd, 'data.spec.configurations.sgPostgresConfig')">
                                <strong class="label">Postgres Configuration</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.configurations.sgPostgresConfig')"></span>
                                <span class="value">
                                    <template v-if="hasProp(crd, 'data.spec.configurations.sgPostgresConfig')"> :
                                        <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + crd.data.spec.configurations.sgPostgresConfig" target="_blank"> 
                                            {{ crd.data.spec.configurations.sgPostgresConfig }}
                                            <span class="eyeIcon"></span>
                                        </router-link>
                                    </template>
                                    <template v-else> : Default</template>
                                </span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') || !crd.data.spec.postgresServices.replicas.enabled || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') || crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">  
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Customize generated Kubernetes service </strong>
                <span class="helpTooltip"  :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices')"></span>
                <ul>
                    <li v-if="showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') || crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Primary Service</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.primary')"></span>
                        <span> : Enabled</span>
                        <ul v-if="( showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') || crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') )">
                            <li v-if="showDefaults">
                                <strong class="label">Name</strong>
                                <span class="helpTooltip" data-tooltip="Specifies the name of Postgres primary service for Distributed Logs"></span>
                                <span class="value"> : {{ crd.data.metadata.name }}-primary.{{crd.data.metadata.namespace}}</span>
                            </li>
                            <li v-if="( showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') )">
                                <strong class="label">Type</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.primary.type')"></span>
                                <span class="value"> : {{ crd.data.spec.postgresServices.primary.type }}</span>
                            </li>
                            <li v-if="crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP')">
                                <strong class="label">Load Balancer IP</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.primary.loadBalancerIP')"></span>
                                <span class="value"> : {{ crd.data.spec.postgresServices.primary.loadBalancerIP }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="showDefaults || !crd.data.spec.postgresServices.replicas.enabled || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') || crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Replicas Service</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.replicas')"></span>
                        <span> : {{ isEnabled(crd.data.spec.postgresServices.replicas.enabled) }}</span>
                        <ul v-if="( showDefaults || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') || crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP'))">
                            <li v-if="showDefaults">
                                <strong class="label">Name</strong>
                                <span class="helpTooltip" data-tooltip="Specifies the name of Postgres replicas service for Distributed Logs"></span>
                                <span class="value"> : {{ crd.data.metadata.name }}-replicas.{{crd.data.metadata.namespace}}</span>
                            </li>
                            <li v-if="( showDefaults || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') )">
                                <strong class="label">Type</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.replicas.type')"></span>
                                <span class="value"> : {{ crd.data.spec.postgresServices.replicas.type }}</span>
                            </li>
                            <li v-if="crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
                                <strong class="label">Load Balancer IP</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.postgresServices.replicas.loadBalancerIP')"></span>
                                <span class="value"> : {{ crd.data.spec.postgresServices.replicas.loadBalancerIP }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="hasProp(crd, 'data.spec.pods.metadata') || hasProp(crd, 'data.spec.metadata.annotations')">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Resources Metadata </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata')"></span>
                <ul>
                <li v-if="hasProp(crd, 'data.spec.metadata.annotations')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Annotations</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations')"></span>
                        <ul>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.allResources')">
                                <button class="toggleSummary"></button>
                                <strong class="label">All Resources</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.allResources')"></span>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.allResources">
                                        <strong class="label">{{ label }}</strong>
                                        <span class="value">: {{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.pods')">
                                <button class="toggleSummary"></button>
                                <strong class="label">Cluster Pods</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.pods')"></span>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.pods">
                                        <strong class="label">{{ label }}</strong>
                                        <span class="value">: {{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.services')">
                                <button class="toggleSummary"></button>
                                <strong class="label">Services</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.services')"></span>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.services">
                                        <strong class="label">{{ label }}</strong>
                                        <span class="value">: {{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            <!-- TO-DO: Once services annotations are implemented on the backend
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.primaryService')">
                                <strong class="sectionTitle">Primary Service</strong>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.primaryService">
                                        <strong class="label">{{ label }}:</strong>
                                        <span class="value">{{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.replicasService')">
                                <strong class="sectionTitle">Replicas Service</strong>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.replicasService">
                                        <strong class="label">{{ label }}:</strong>
                                        <span class="value">{{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            -->
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
        
        <ul class="section" v-if="hasProp(crd, 'data.spec.scheduling')">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Scheduling </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling')"></span>
                <ul>
                    <li v-if="hasProp(crd, 'data.spec.scheduling.nodeSelector')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Selectors</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.nodeSelector')"></span>
                        <ul>
                            <li v-for="(value, key) in crd.data.spec.scheduling.nodeSelector">
                                <strong class="label">{{ key }}</strong>
                                <span class="value">: {{ value }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="hasProp(crd, 'data.spec.scheduling.tolerations')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Toleration</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations')"></span>
                        <ul>
                            <li v-for="(toleration, index) in crd.data.spec.scheduling.tolerations">
                                <button class="toggleSummary"></button>
                                <strong class="label">Toleration #{{ index+1 }}</strong>
                                <ul>
                                    <li>
                                        <strong class="label">Key</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.key')"></span>
                                        <span class="value"> : {{ toleration.key }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Operator</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.operator')"></span>
                                        <span class="value"> : {{ toleration.operator }}</span>
                                    </li>
                                    <li v-if="toleration.hasOwnProperty('value')">
                                        <strong class="label">Value</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.value')"></span>
                                        <span class="value"> : {{ toleration.value }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Effect</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.effect')"></span>
                                        <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                    </li>
                                    <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                        <strong class="label">Toleration Seconds</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.tolerationSeconds')"></span>
                                        <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="showDefaults || (hasProp(crd, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (crd.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null))">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Non Production Settings </strong>
                <ul>
                    <li>
                        <strong class="label">Cluster Pod Anti Affinity</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('If set to `true` it will allow','When disabled, it allows running')"></span>
                        <span> : {{ (hasProp(crd, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') && (crd.data.spec.nonProductionOptions.disableClusterPodAntiAffinity != null)) ? isEnabled(crd.data.spec.nonProductionOptions.disableClusterPodAntiAffinity, true) : 'Default'}}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="crd.data.hasOwnProperty('status') && crd.data.status.clusters.length">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li class="usedOn">
                        <button class="toggleSummary"></button>
                        <strong class="label">Used on</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.status.clusters')"></span>
                        <ul>    
                            <li v-for="cluster in crd.data.status.clusters">
                                <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
                                    {{ cluster }}
                                    <span class="eyeIcon"></span>
                                </router-link>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
import store from '../../../store'
import {mixin} from '../../mixins/mixin'

    export default {
        name: 'SGDistributedLogsSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],

        computed: {

            profiles () {
				return store.state.sginstanceprofiles
			}
            
		}
	}
</script>