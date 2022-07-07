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
                        <strong class="sectionTitle">Storage</strong>
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
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="showDefaults || hasProp(crd, 'data.spec.postgresServices')">
            <li>
                <strong class="sectionTitle">Customize generated Kubernetes service</strong>
                <ul>
                    <li v-if="showDefaults || hasProp(crd, 'data.spec.postgresServices.primary')">
                        <strong class="sectionTitle">Primary Service</strong>
                        <ul>
                            <li v-if="( showDefaults || hasProp(crd, 'data.spec.postgresServices.primary.enabled') )">
                                <strong class="label">Enable:</strong>
                                <span class="value">{{ hasProp(crd, 'data.spec.postgresServices.primary.enabled') ? (crd.data.spec.postgresServices.primary.enabled ? 'YES' : 'NO') : 'YES' }}</span>
                            </li>
                            <li v-if="( showDefaults || hasProp(crd, 'data.spec.postgresServices.primary.type') )">
                                <strong class="label">Type:</strong>
                                <span class="value">{{ hasProp(crd, 'data.spec.postgresServices.primary.type') ? crd.data.spec.postgresServices.primary.type : 'ClusterIP' }}</span>
                            </li>
                        </ul>
                    </li>
                    <li v-if="showDefaults || hasProp(crd, 'data.spec.postgresServices.replicas')">
                        <strong class="sectionTitle">Replicas Service</strong>
                        <ul>
                            <li v-if="( showDefaults || hasProp(crd, 'data.spec.postgresServices.replicas.enabled') )">
                                <strong class="label">Enable:</strong>
                                <span class="value">{{ hasProp(crd, 'data.spec.postgresServices.replicas.enabled') ? (crd.data.spec.postgresServices.replicas.enabled ? 'YES' : 'NO') : 'YES' }}</span>
                            </li>
                            <li v-if="( showDefaults || hasProp(crd, 'data.spec.postgresServices.replicas.type') )">
                                <strong class="label">Type:</strong>
                                <span class="value">{{ hasProp(crd, 'data.spec.postgresServices.replicas.type') ? crd.data.spec.postgresServices.replicas.type : 'ClusterIP' }}</span>
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
                        <strong class="sectionTitle">Cluster Pod Anti Affinity</strong>
                        <ul>
                            <li>
                                <strong class="label">Enable:</strong>
                                <span class="value">{{ hasProp(crd, 'data.spec.nonProductionOptions.disableClusterPodAntiAffinity') ? (crd.data.spec.nonProductionOptions.disableClusterPodAntiAffinity ? 'NO' : 'YES') : 'YES' }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
import {mixin} from '../../mixins/mixin'

    export default {
        name: 'SGDistributedLogsSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults']
	}
</script>