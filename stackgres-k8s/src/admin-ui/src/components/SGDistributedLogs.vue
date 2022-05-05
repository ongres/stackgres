<template>
    <div id="logs-cluster" v-if="loggedIn && isReady && !notFound">
        <div class="content">
            <template v-if="!$route.params.hasOwnProperty('name')">
                <table id="logs" class="logsCluster pgConfig resizable fullWidth" v-columns-resizable>
                    <thead class="sort">
                        <th class="sorted desc name hasTooltip">
                            <span @click="sort('data.metadata.name')" title="Name">
                                Name
                            </span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.metadata.name')"></span>
                        </th>
                        <th class="desc volumeSize hasTooltip textRight">
                            <span @click="sort('data.spec.persistentVolume.size', 'memory')" title="Volume Size">
                                Volume Size
                            </span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.size')"></span>
                        </th>
                        <th class="actions"></th>
                    </thead>
                    <tbody>
                        <template v-if="!clusters.length">
							<tr class="no-results">
								<td :colspan="3" v-if="iCan('create','sgdistributedlogs',$route.params.namespace)">
                                    No logs servers have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgdistributedlogs/new'" title="Add New Logs Server">create a new one?</router-link>
                                </td>
                                <td v-else colspan="3">
                                    No logs servers have been found. You don't have enough permissions to create a new one
                                </td>
							</tr>
						</template>
                        <template v-for="(cluster, index) in clusters">
                            <template  v-if="(index >= pagination.start) && (index < pagination.end)">
                                <tr class="base">
                                    <td class="hasTooltip clusterName">
                                        <span>
                                            <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" class="noColor">
                                                {{ cluster.name }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="volumeSize fontZero textRight">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" class="noColor">
                                            {{ cluster.data.spec.persistentVolume.size }}
                                        </router-link>
                                    </td>
                                    <td class="actions">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.name" target="_blank" class="newTab"></router-link>
                                        <router-link v-if="iCan('patch','sgdistributedlogs',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgdistributedlog/' + cluster.data.metadata.name + '/edit'" title="Edit Configuration" class="editCRD"></router-link>
                                        <a v-if="iCan('create','sgdistributedlogs',$route.params.namespace)" @click="cloneCRD('SGDistributedLogs', $route.params.namespace, cluster.data.metadata.name)" class="cloneCRD" title="Clone Logs Server"></a>
                                        <a v-if="iCan('delete','sgdistributedlogs',$route.params.namespace)" @click="deleteCRD('sgdistributedlogs',$route.params.namespace, cluster.data.metadata.name)" class="delete deleteCRD" title="Delete Configuration" :class="cluster.data.status.clusters.length ? 'disabled' : ''"></a>
                                    </td>
                                </tr>
                            </template>
                        </template>
                    </tbody>
                </table>
                <v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < clusters.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="clusters.length" @page-change="pageChange" align="center" ref="page"></v-page>
                <div id="nameTooltip">
                    <div class="info"></div>
                </div>
            </template>
            <template v-else>
                <h2>Logs Server Details</h2>
                <template v-for="cluster in clusters" v-if="cluster.name == $route.params.name">
                    <div class="configurationDetails">
                        <table class="crdDetails">
                            <tbody>
                                <tr>
									<td class="label">Name <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.metadata.name')"></span></td>
									<td colspan="2">{{ cluster.name }}</td>
								</tr>
                                <tr>
                                    <td class="label" :rowspan="cluster.data.spec.persistentVolume.hasOwnProperty('storageClass') ? '2' : ''">
                                        Persistent Volume
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume')"></span>
                                    </td>
                                    <td class="label">
                                        Volume Size
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.size')"></span>
                                    </td>
                                    <td class="textRight">{{ cluster.data.spec.persistentVolume.size }}</td>
                                </tr>
                                <tr v-if="cluster.data.spec.persistentVolume.hasOwnProperty('storageClass')">
                                    <td class="label">
                                        Storage Class
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.persistentVolume.storageClass')"></span>
                                    </td>
                                    <td>{{ cluster.data.spec.persistentVolume.storageClass }}</td>
                                </tr>
                                <tr v-if="(typeof cluster.data.spec.nonProductionOptions !== 'undefined')">
                                    <td class="label">
                                        Non-Production Settings
                                    </td>
                                    <td class="label">
                                        Cluster Pod Anti Affinity
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.nonProductionOptions.disableClusterPodAntiAffinity').replace('If set to `true` it will allow','When disabled, it allows running')"></span>
                                    </td>
                                    <td>{{ cluster.data.spec.nonProductionOptions.disableClusterPodAntiAffinity ? 'OFF' : 'ON' }}</td>
                                </tr>
                                <tr v-if="cluster.data.status.clusters.length">
									<td class="label">Used on  <span class="helpTooltip" :data-tooltip="getTooltip('sgpoosgdistributedlogslingconfig.status.clusters')"></span></td>
									<td class="usedOn" colspan="2">
										<ul>
											<li v-for="cluster in cluster.data.status.clusters">
												<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster" title="Cluster Details">
													{{ cluster }}
													<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
												</router-link>
											</li>
										</ul>
									</td>
								</tr>
                            </tbody>
                        </table>

                        <template v-if="hasProp(cluster, 'data.spec.metadata.annotations')">
                            <h2>
                                Resources Metadata
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations')"></span>
                            </h2>
                            <table class="crdDetails">
                                <tbody>
                                    <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.allResources)" v-if="hasProp(cluster, 'data.spec.metadata.annotations.allResources')">
                                        <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.allResources).length">
                                            All Resources
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.allResources')"></span>
                                        </td>
                                        <td class="label">
                                            {{ item.annotation }}
                                        </td>
                                        <td colspan="2">
                                            {{ item.value }}
                                        </td>
                                    </tr>
                                    <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.pods)" v-if="hasProp(cluster, 'data.spec.metadata.annotations.pods')">
                                        <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.pods).length">
                                            Pods
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.pods')"></span>
                                        </td>
                                        <td class="label">
                                            {{ item.annotation }}
                                        </td>
                                        <td colspan="2">
                                            {{ item.value }}
                                        </td>
                                    </tr>
                                    <tr v-for="(item, index) in unparseProps(cluster.data.spec.metadata.annotations.services)" v-if="hasProp(cluster, 'data.spec.metadata.annotations.services')">
                                        <td v-if="!index" class="label" :rowspan="Object.keys(cluster.data.spec.metadata.annotations.services).length">
                                            Services
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.metadata.annotations.services')"></span>
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
                        </template>	
                    </div>

                    <div class="configurationDetails">

                        <div class="postgresServices" v-if="hasProp(cluster, 'data.spec.postgresServices') && ( hasProp(cluster, 'data.spec.postgresServices.primary') || hasProp(cluster, 'data.spec.postgresServices.replicas') )">
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
                        
                        <table v-if="hasProp(cluster, 'data.spec.scheduling.nodeSelector')" class="crdDetails">
                            <thead>
                                <th colspan="2" class="label">
                                    Node Selector
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.nodeSelector')"></span>
                                </th>
                            </thead>
                            <tbody>
                                <tr v-for="(item, index) in unparseProps(cluster.data.spec.scheduling.nodeSelector)">
                                    <td class="label">
                                        {{ item.annotation }}
                                    </td>
                                    <td colspan="2">
                                        {{ item.value }}
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    
                        <table v-if="hasProp(cluster, 'data.spec.scheduling.tolerations')" class="crdDetails">
                            <thead>
                                <th colspan="3" class="label">
                                    Tolerations
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations')"></span>
                                </th>
                            </thead>
                            <tbody>
                                <template v-for="(item, index) in cluster.data.spec.scheduling.tolerations">
                                    <tr v-for="(value, prop, i) in item">
                                        <td class="label" :rowspan="Object.keys(item).length" v-if="!i">
                                            Toleration #{{ index+1 }}
                                        </td>
                                        <td class="label">
                                            {{ prop }}
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdistributedlogs.spec.scheduling.tolerations.'+prop)"></span>
                                        </td>
                                        <td colspan="2" :class="prop">
                                            {{ value }}
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>
                    
                    </div>
                </template>
            </template>
        </div>
    </div>
</template>

<script>
    import { mixin } from './mixins/mixin'
    import store from '../store'

    export default {
        name: 'SGDistributedLogs',

        mixins: [mixin],

        data: function() {
            return {
                currentSort: {
					param: 'data.metadata.name',
					type: 'alphabetical'
				},
				currentSortDir: 'asc',
            }
        },
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

        },
    
        computed: {

            clusters () {
                return this.sortTable([...(store.state.sgdistributedlogs.filter(cluster => (cluster.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type )
            },
            
            tooltips() {
                return store.state.tooltips
            }
        }
    }
</script>

<style scoped>
    td.tolerationSeconds {
		text-align: right;
	}
</style>