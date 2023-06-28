<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.metadata.namespace')"></span> 
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Specs </strong>
                <ul>
                    <li>
                        <strong class="label">RAM</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span>
                        <span class="value"> : {{ crd.data.spec.memory }}</span>
                    </li>
                    <li>
                        <strong class="label">CPU</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span>
                        <span class="value"> : {{ crd.data.spec.cpu }}</span>
                    </li>
                    <li v-if="( crd.data.spec.hasOwnProperty('hugePages') && (crd.data.spec.hugePages != null) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Huge Pages</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages')"></span>
                        <ul>
                            <li v-if="crd.data.spec.hugePages.hasOwnProperty('hugepages-2Mi')">
                                <strong class="label">Huge Pages 2Mi</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.hugepages-2Mi')"></span>
                                <span class="value"> : {{ crd.data.spec.hugePages['hugepages-2Mi'] }}</span>
                            </li>
                            <li v-if="crd.data.spec.hugePages.hasOwnProperty('hugepages-1Gi')">
                                <strong class="label">Huge Pages 1Gi</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.hugePages.hugepages-1Gi')"></span>
                                <span class="value"> : {{ crd.data.spec.hugePages['hugepages-1Gi'] }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.hasOwnProperty('containers') && (crd.data.spec.containers != null) )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Containers </strong> 
                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers')"></span>
                <ul>
                    <li v-for="(container, containerName) in crd.data.spec.containers">
                        <button class="toggleSummary"></button>
                        <strong class="label">{{ containerName }}</strong>
                        <ul>
                            <li>
                                <strong class="label">RAM</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.memory')"></span>
                                <span class="value"> : {{ container.memory }}</span>
                            </li>
                            <li>
                                <strong class="label">CPU</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.cpu')"></span>
                                <span class="value"> : {{ container.cpu }}</span>
                            </li>
                            <li v-if="container.hasOwnProperty('hugePages')">
                                <button class="toggleSummary"></button>
                                <strong class="label">Huge Pages</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.hugePages')"></span>
                                <ul>
                                    <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                        <strong class="label">Huge Pages 2Mi</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.hugePages.properties.hugepages-2Mi')"></span>
                                        <span class="value"> : {{ container.hugePages['hugepages-2Mi'] }}</span>
                                    </li>
                                    <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                        <strong class="label">Huge Pages 1Gi</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.containers.additionalProperties.properties.hugePages.properties.hugepages-1Gi')"></span>
                                        <span class="value"> : {{ container.hugePages['hugepages-1Gi'] }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.hasOwnProperty('initContainers') && (crd.data.spec.initContainers != null) )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Init Containers </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers')"></span>
                <ul>
                    <li v-for="(container, containerName) in crd.data.spec.initContainers">
                        <button class="toggleSummary"></button>
                        <strong class="label">{{ containerName }}</strong>
                        <ul>
                            <li>
                                <strong class="label">RAM</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.memory')"></span>
                                <span class="value"> : {{ container.memory }}</span>
                            </li>
                            <li>
                                <strong class="label">CPU</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.cpu')"></span>
                                <span class="value"> : {{ container.cpu }}</span>
                            </li>
                            <li v-if="container.hasOwnProperty('hugePages')">
                                <button class="toggleSummary"></button>
                                <strong class="label">Huge Pages</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.hugePages')"></span>
                                <ul>
                                    <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                        <strong class="label">Huge Pages 2Mi</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.hugePages.properties.hugepages-2Mi')"></span>
                                        <span class="value"> : {{ container.hugePages['hugepages-2Mi'] }}</span>
                                    </li>
                                    <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                        <strong class="label">Huge Pages 1Gi</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.initContainers.additionalProperties.properties.hugePages.properties.hugepages-1Gi')"></span>
                                        <span class="value"> : {{ container.hugePages['hugepages-1Gi'] }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( crd.data.spec.hasOwnProperty('requests') && (crd.data.spec.requests != null) )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Requests </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests')"></span>
                <ul>
                    <li>
                        <strong class="label">RAM</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.memory')"></span>
                        <span class="value"> : {{ crd.data.spec.requests.memory }}</span>
                    </li>
                    <li>
                        <strong class="label">CPU</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.cpu')"></span>
                        <span class="value"> : {{ crd.data.spec.requests.cpu }}</span>
                    </li>
                    <li v-if="(crd.data.spec.requests.hasOwnProperty('containers') && (crd.data.spec.requests.containers != null) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Containers</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers')"></span>
                        <ul>
                            <li v-for="(container, containerName) in crd.data.spec.requests.containers">
                                <button class="toggleSummary"></button>
                                <strong class="label">{{ containerName }}</strong>
                                <ul>
                                    <li>
                                        <strong class="label">RAM</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.memory')"></span>
                                        <span class="value"> : {{ container.memory }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">CPU</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.cpu')"></span>
                                        <span class="value"> : {{ container.cpu }}</span>
                                    </li>
                                    <li v-if="container.hasOwnProperty('hugePages')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Huge Pages</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.hugePages')"></span>
                                        <ul>
                                            <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                                <strong class="label">Huge Pages 2Mi</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.hugePages.hugepages-2Mi')"></span>
                                                <span class="value"> : {{ container.hugePages['hugepages-2Mi'] }}</span>
                                            </li>
                                            <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                                <strong class="label">Huge Pages 1Gi</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.containers.additionalProperties.properties.hugePages.hugepages-1Gi')"></span>
                                                <span class="value"> : {{ container.hugePages['hugepages-1Gi'] }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                    <li v-if="( crd.data.spec.requests.hasOwnProperty('initContainers') && (crd.data.spec.requests.initContainers != null) )">
                        <button class="toggleSummary"></button>
                        <strong class="label">Init Containers</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers')"></span>
                        <ul>
                            <li v-for="(container, containerName) in crd.data.spec.requests.initContainers">
                                <button class="toggleSummary"></button>
                                <strong class="label">{{ containerName }}</strong>
                                <ul>
                                    <li>
                                        <strong class="label">RAM</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.memory')"></span>
                                        <span class="value"> : {{ container.memory }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">CPU</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.cpu')"></span>
                                        <span class="value"> : {{ container.cpu }}</span>
                                    </li>
                                    <li v-if="container.hasOwnProperty('hugePages')">
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Huge Pages</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.hugePages')"></span>
                                        <ul>
                                            <li v-if="container.hugePages.hasOwnProperty('hugepages-2Mi')">
                                                <strong class="label">Huge Pages 2Mi</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.hugePages.hugepages-2Mi')"></span>
                                                <span class="value"> : {{ container.hugePages['hugepages-2Mi'] }}</span>
                                            </li>
                                            <li v-if="container.hugePages.hasOwnProperty('hugepages-1Gi')">
                                                <strong class="label">Huge Pages 1Gi</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.requests.initContainers.additionalProperties.properties.hugePages.hugepages-1Gi')"></span>
                                                <span class="value"> : {{ container.hugePages['hugepages-1Gi'] }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.hasOwnProperty('status') && (crd.data.status.clusters.length || logsClusters.length))">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li class="usedOn">
                        <button class="toggleSummary"></button>
                        <strong class="label">Used on</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.status.clusters').replace('[SGClusters](https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/)', 'resources')"></span>
                        <ul>
                            <template v-if="crd.data.status.clusters.length">
                                <li>
                                    <button class="toggleSummary"></button>
                                    <strong class="label">SGClusters</strong>
                                    <ul>
                                        <li v-for="cluster in crd.data.status.clusters">
                                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
                                                {{ cluster }}
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </li>
                                    </ul>
                                </li>
                            </template>
                            <template v-if="logsClusters.length">
                                <li>
                                    <button class="toggleSummary"></button>
                                    <strong class="label">SGDistributedLogs</strong>
                                    <ul>
                                        <li v-for="lcluster in logsClusters">
                                            <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + lcluster.name" title="Logs Server Details">
                                                {{ lcluster.name }}
                                                <span class="eyeIcon"></span>
                                            </router-link>
                                        </li>
                                    </ul>
                                </li>
                            </template>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import { mixin } from './../../mixins/mixin'
    import store from '../../../store'
	
    export default {
        name: 'SGInstanceProfileSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],
        
        computed: {
			logsClusters(){
                return store.state.sgdistributedlogs.filter(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.spec.sgInstanceProfile == this.$route.params.name))
            }              
		}
    }
</script>