<template>
	<div id="cluster-overview" v-if="iCanLoad">
		<template v-for="namespace in namespaces" v-if="(namespace == $route.params.namespace)">
			<div class="content">
				
				<table class="clusterOverview resizable fullWidth" v-if="iCan('list','sgclusters',$route.params.namespace)" v-columns-resizable>
					<thead class="sort">
						<th class="sorted asc name hasTooltip">
							<span @click="sort('data.metadata.name')" title="StackGres Cluster">StackGres Cluster</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.metadata.name')"></span>
						</th>

						<th class="asc instances hasTooltip textRight">
							<span @click="sort('data.spec.instances')" title="Instances">Instances</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.instances')"></span>
						</th>

						<th class="asc cpu hasTooltip textRight">
							<span @click="sort('status.cpuRequested', 'cpu')" title="CPU">CPU</span>
							<span class="helpTooltip"  :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span>
						</th>

						<th class="asc memory hasTooltip textRight">
							<span @click="sort('status.memoryRequested', 'memory')" title="Memory">Memory</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span>
						</th>

						<th class="asc disk hasTooltip textRight">
							<span @click="sort('data.spec.pods.persistentVolume.size', 'memory')" title="Disk">Disk</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.spec.pods.persistentVolume.size')"></span>
						</th>

						<th class="notSortable hasTooltip textRight">
							<span title="Health">Health</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.podsReady').slice(0, -2) + ' / ' + getTooltip('sgcluster.spec.instances')"></span>
						</th>
						
						<th class="actions"></th>
					</thead>
					<tbody>
						<template v-if="!clusters.length">
							<tr class="no-results">
								<td colspan="7" v-if="iCan('create','sgclusters',$route.params.namespace)">
									No clusters have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgclusters/new'" title="Add New Cluster">create a new one?</router-link>
								</td>
								<td v-else colspan="7">
									No clusters have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>		
						<template v-else>
							<template v-for="(cluster, index) in clusters">
								<template v-if="(index >= pagination.start) && (index < pagination.end)">
									<tr class="base">
										<td class="clusterName hasTooltip">											
											<template v-if="hasProp(cluster, 'data.status.conditions')">
												<template v-for="condition in cluster.data.status.conditions" v-if="( (condition.type == 'PendingRestart') && (condition.status == 'True') )">
													<div class="helpTooltip alert onHover" data-tooltip="A restart operation is pending for this cluster"></div>
												</template>
											</template>
											<span>
												<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" title="Cluster Status" data-active=".set.clu" class="noColor">
													{{ cluster.name }}
												</router-link>	
											</span>
										</td>
										<td class="instances textRight">
											<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" title="Cluster Status" data-active=".set.clu" class="noColor">
												{{ cluster.data.spec.instances }}
											</router-link>
										</td>
										<td class="cpu textRight">
											<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" title="Cluster Status" data-active=".set.clu" class="noColor">
												<span>{{ hasProp(cluster,'status.cpuRequested') ? cluster.status.cpuRequested : ''}}</span>
											</router-link>
										</td>
										<td class="ram textRight">
											<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" title="Cluster Status" data-active=".set.clu" class="noColor">
												<span>{{ hasProp(cluster,'status.memoryRequested') ? cluster.status.memoryRequested.replace('.00','') : '' }}</span>
											</router-link>
										</td>
										<td class="volumeSize textRight">
											<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" title="Cluster Status" data-active=".set.clu" class="noColor">
												{{ cluster.data.spec.pods.persistentVolume.size }}
											</router-link>
										</td>
										<td class="health textRight">
											<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" title="Cluster Status" data-active=".set.clu" class="noColor">
												{{ cluster.data.podsReady }} / {{ cluster.data.spec.instances }}
											</router-link>
										</td>
										<td class="actions">
											<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name" target="_blank" class="newTab"></router-link>
											<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name + '/edit'" title="Edit Cluster" data-active=".set.clu" class="editCRD" :class="isSharded(cluster.name) && 'disabled'"></router-link>
											<a v-if="iCan('create','sgclusters',$route.params.namespace)" @click="cloneCRD('SGClusters', $route.params.namespace, cluster.name)" class="cloneCRD" :class="isSharded(cluster.name) && 'disabled'" title="Clone Cluster"></a>
											<a @click="setRestartCluster($route.params.namespace, cluster.name)" class="restartCluster" title="Restart Cluster"></a>
											<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgclusters', $route.params.namespace, cluster.name)" title="Delete Cluster" class="deleteCRD" :class="isSharded(cluster.name) && 'disabled'"></a>
										</td>
									</tr>
								</template>
							</template>
						</template>
					</tbody>
				</table>
				<v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < clusters.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="clusters.length" @page-change="pageChange" align="center" ref="page"></v-page>
			</div>
		</template>
		<div id="nameTooltip">
			<div class="info"></div>
		</div>
	</div>
</template>

<script>
	import store from '../store'
	import { mixin } from './mixins/mixin'

    export default {
        name: 'ClusterOverview',

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
		computed: {
			clusters () {
				return this.sortTable([...(store.state.sgclusters.filter(cluster => (cluster.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type)
			},

			namespaces() {
				return store.state.allNamespaces
			},

			profiles () {
				return store.state.sginstanceprofiles
			},

			tooltips () {
				return store.state.tooltips
			}
		},
		methods: {
			isSharded(cluster) {
				let shards = store.state.sgshardedclusters.filter(cluster => (cluster.data.metadata.namespace == this.$route.params.namespace))		
				
				return typeof(shards.find((c) => (c.data.status.clusters.includes(cluster)))) != 'undefined'
			}
		}
	}
</script>

<style scoped>
	.clusterOverview td.actions {
		padding: 0 10px;
	} 

	.clusterOverview td > a {
   		padding: 12px 0;
	}

	.clusterName .helpTooltip.alert {
		width: 32px;
		height: 32px;
		left: auto;
		right: 0;
		position: absolute;
	}

	td.clusterName > div + span {
		width: calc(100% - 45px);
	}

	th.actions, td.actions {
		width: 143px !important;
		min-width: 143px;
		max-width: 143px;
	}

	a.cloneCRD svg {
		position: relative;
		top: 1px;
	}

	td.actions a {
		top: 7px;
	}
</style>
