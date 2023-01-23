<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Cluster</strong>
                <ul>
                    <li>
                        <strong class="sectionTitle">Metadata</strong>
                        <ul>
                            <li v-if="showDefaults">
                                <strong class="label">Namespace:</strong>
                                <span class="value">{{ crd.data.metadata.namespace }}</span>
                            </li>
                            <li>
                                <strong class="label">Name:</strong>
                                <span class="value">{{ crd.data.metadata.name }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="showDefaults || (crd.data.spec.persistentVolume.size != '1Gi') || hasProp(crd, 'data.spec.persistentVolume.storageClass')">
                        <strong class="sectionTitle">Persistent Volume</strong>
                        <ul>
                            <li v-if="showDefaults || (crd.data.spec.persistentVolume.size != '1Gi')">
                                <strong class="label">Volume Size:</strong>
                                <span class="value">{{ crd.data.spec.persistentVolume.size }}B</span>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.persistentVolume.storageClass')">
                                <strong class="label">Storage Class:</strong>
                                <span class="value">{{ crd.data.spec.persistentVolume.storageClass }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="showDefaults || hasProp(crd, 'data.spec.sgInstanceProfile') || hasProp(crd, 'data.spec.configurations.sgPostgresConfig')">
                        <strong class="sectionTitle">Pods Resources</strong>
                        <ul>
                            <li v-if="showDefaults || hasProp(crd, 'data.spec.sgInstanceProfile')">
                                <strong class="label">Instance Profile:</strong>
                                <span class="value">
                                    <template v-if="hasProp(crd, 'data.spec.sgInstanceProfile')">
                                        <router-link :to="'/' + $route.params.namespace + '/sginstanceprofile/' + profile.name" target="_blank" v-for="profile in profiles" v-if="( (profile.name == crd.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == crd.data.metadata.namespace) )"> 
                                            {{ profile.data.metadata.name }} (Cores: {{ profile.data.spec.cpu }}, RAM: {{ profile.data.spec.memory }})
                                        </router-link>
                                    </template>
                                    <template v-else>
                                        Default (Cores: 1, RAM: 2GiB)
                                    </template>
                                </span>
                            </li>
                            <li v-if="showDefaults || hasProp(crd, 'data.spec.configurations.sgPostgresConfig')">
                                <strong class="label">Postgres Configuration:</strong>
                                <span class="value">
                                    <template v-if="hasProp(crd, 'data.spec.configurations.sgPostgresConfig')">
                                        <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + crd.data.spec.configurations.sgPostgresConfig" target="_blank"> 
                                            {{ crd.data.spec.configurations.sgPostgresConfig }}
                                        </router-link>
                                    </template>
                                    <template v-else>
                                        Default
                                    </template>
                                </span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') || !crd.data.spec.postgresServices.replicas.enabled || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') || crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') || crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
            <li>
                <strong class="sectionTitle">Customize generated Kubernetes service</strong>
                <ul>
                    <li v-if="showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') || crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP')">
                        <strong class="sectionTitle">Primary Service</strong>
                        <span><strong>:</strong> Enabled</span>
                        <ul v-if="( showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') || crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP') )">
                            <li v-if="( showDefaults || (crd.data.spec.postgresServices.primary.type != 'ClusterIP') )">
                                <strong class="label">Type:</strong>
                                <span class="value">{{ crd.data.spec.postgresServices.primary.type }}</span>
                            </li>
                            <li v-if="crd.data.spec.postgresServices.primary.hasOwnProperty('loadBalancerIP')">
                                <strong class="label">Load Balancer IP:</strong>
                                <span class="value">{{ crd.data.spec.postgresServices.primary.loadBalancerIP }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="showDefaults || !crd.data.spec.postgresServices.replicas.enabled || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') || crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
                        <strong class="sectionTitle">Replicas Service</strong>
                        <span><strong>:</strong> {{ isEnabled(crd.data.spec.postgresServices.replicas.enabled) }}</span>
                        <ul v-if="( showDefaults || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') || crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP'))">
                            <li v-if="( showDefaults || (crd.data.spec.postgresServices.replicas.type != 'ClusterIP') )">
                                <strong class="label">Type:</strong>
                                <span class="value">{{ crd.data.spec.postgresServices.replicas.type }}</span>
                            </li>
                            <li v-if="crd.data.spec.postgresServices.replicas.hasOwnProperty('loadBalancerIP')">
                                <strong class="label">Load Balancer IP:</strong>
                                <span class="value">{{ crd.data.spec.postgresServices.replicas.loadBalancerIP }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="hasProp(crd, 'data.spec.pods.metadata') || hasProp(crd, 'data.spec.metadata.annotations')">
            <li>
                <strong class="sectionTitle">Metadata</strong>
                <ul>
                <li v-if="hasProp(crd, 'data.spec.metadata.annotations')">
                        <strong class="sectionTitle">Annotations</strong>
                        <ul>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.allResources')">
                                <strong class="sectionTitle">All Resources</strong>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.allResources">
                                        <strong class="label">{{ label }}:</strong>
                                        <span class="value">{{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.pods')">
                                <strong class="sectionTitle">Cluster Pods</strong>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.pods">
                                        <strong class="label">{{ label }}:</strong>
                                        <span class="value">{{ value }}</span>
                                    </li>
                                </ul>
                            </li>
                            <li v-if="hasProp(crd, 'data.spec.metadata.annotations.services')">
                                <strong class="sectionTitle">Services</strong>
                                <ul>
                                    <li v-for="(value, label) in crd.data.spec.metadata.annotations.services">
                                        <strong class="label">{{ label }}:</strong>
                                        <span class="value">{{ value }}</span>
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
                <strong class="sectionTitle">Scheduling</strong>
                <ul>
                    <li v-if="hasProp(crd, 'data.spec.scheduling.nodeSelector')">
                        <strong class="sectionTitle">Node Selectors</strong>
                        <ul>
                            <li v-for="(value, key) in crd.data.spec.scheduling.nodeSelector">
                                <strong class="label">{{ key }}:</strong>
                                <span class="value">{{ value }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="hasProp(crd, 'data.spec.scheduling.tolerations')">
                        <strong class="sectionTitle">Node Tolerations</strong>
                        <ul>
                            <li v-for="(toleration, index) in crd.data.spec.scheduling.tolerations">
                                <strong class="sectionTitle">Toleration #{{ index+1 }}</strong>
                                <ul>
                                    <li>
                                        <strong class="label">Key:</strong>
                                        <span class="value">{{ toleration.key }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Operator:</strong>
                                        <span class="value">{{ toleration.operator }}</span>
                                    </li>
                                    <li v-if="toleration.hasOwnProperty('value')">
                                        <strong class="label">Value:</strong>
                                        <span class="value">{{ toleration.value }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Effect:</strong>
                                        <span class="value">{{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                    </li>
                                    <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                        <strong class="label">Toleration Seconds:</strong>
                                        <span class="value">{{ toleration.tolerationSeconds }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="showDefaults || hasProp(crd, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity')">
            <li>
                <strong class="sectionTitle">Non Production Settings</strong>
                <ul>
                    <li>
                        <strong class="sectionTitle">Cluster Pod Anti Affinity: </strong>
                        <span>{{ hasProp(crd, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? isEnabled(crd.data.spec.nonProductionOptions.disableClusterPodAntiAffinity, true) : 'Enabled' }}</span>
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