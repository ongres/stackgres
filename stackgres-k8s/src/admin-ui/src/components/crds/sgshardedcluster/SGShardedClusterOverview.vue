<template>
	<div id="sgshardedcluster-overview" v-if="iCanLoad">
		<div class="content">
			
			<table class="clusterOverview resizable fullWidth" v-if="iCan('list','sgshardedclusters',$route.params.namespace)" v-columns-resizable>
				<thead class="sort">
					<th class="sorted asc name hasTooltip">
						<span @click="sort('data.metadata.name')" title="StackGres Sharded Cluster">StackGres Sharded Cluster</span>
						<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.metadata.name')"></span>
					</th>

					<th class="asc instances hasTooltip textRight">
						<span @click="sort('data.spec.coordinator.instances')" title="Instances">Coordinator Instances</span>
						<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.instances')"></span>
					</th>

					<th class="asc instances hasTooltip textRight">
						<span @click="sort('data.spec.shards.clusters')" title="Instances">Clusters / Instances per Shard</span>
						<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.shards.clusters') + ' / ' + getTooltip('sgshardedcluster.spec.shards.instancesPerCluster')"></span>
					</th>

					<th class="asc cpu hasTooltip textRight">
						<span @click="sort('stats.coordinator.cpuRequested', 'cpu')" title="CPU">CPU</span>
						<span class="helpTooltip"  :data-tooltip="getTooltip('sgprofile.spec.cpu')"></span>
					</th>

					<th class="asc memory hasTooltip textRight">
						<span @click="sort('stats.coordinator.memoryRequested', 'memory')" title="Memory">Memory</span>
						<span class="helpTooltip" :data-tooltip="getTooltip('sgprofile.spec.memory')"></span>
					</th>

					<th class="disk hasTooltip textRight">
						<span title="Disk">Disk</span>
						<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume.size')"></span>
					</th>

					<th class="actions"></th>
				</thead>
				<tbody>
					<template v-if="!sgshardedclusters.length">
						<tr class="no-results">
							<td colspan="7" v-if="iCan('create','sgshardedclusters',$route.params.namespace)">
								No sharded clusters have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgshardedclusters/new'" title="Add New Cluster">create a new one?</router-link>
							</td>
							<td v-else colspan="7">
								No sharded clusters have been found. You don't have enough permissions to create a new one
							</td>
						</tr>
					</template>		
					<template v-else>
						<template v-for="(cluster, index) in sgshardedclusters">
							<template v-if="index >= pagination.start && index < pagination.end">
								<tr class="base">
									<td class="clusterName hasTooltip">											
										<template v-if="hasProp(cluster, 'data.status.conditions')">
											<template v-for="condition in cluster.data.status.conditions" v-if="( (condition.type == 'PendingRestart') && (condition.status == 'True') )">
												<div class="helpTooltip alert onHover" data-tooltip="A restart operation is pending for this sharded cluster"></div>
											</template>
										</template>
										<span>
											<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" title="Sharded Cluster Status" class="noColor">
												{{ cluster.name }}
											</router-link>	
										</span>
									</td>
									<td class="instances textRight">
										<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" title="Cluster Status" class="noColor">
											{{ cluster.data.spec.coordinator.instances }}
										</router-link>
									</td>
									<td class="instances textRight">
										<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" title="Cluster Status" class="noColor">
											{{ cluster.data.spec.shards.clusters }} / {{ cluster.data.spec.shards.instancesPerCluster }}
										</router-link>
									</td>
									<td class="cpu textRight">
										<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" title="Cluster Status" class="noColor">
											<span>{{ hasProp(cluster,'stats.coordinator.cpuRequested') ? cluster.stats.coordinator.cpuRequested : '-'}} / {{ hasProp(cluster,'stats.shards.cpuRequested') ? cluster.stats.shards.cpuRequested : '-'}}</span>
										</router-link>
									</td>
									<td class="ram textRight">
										<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" title="Cluster Status" class="noColor">
											<span>{{ hasProp(cluster,'stats.coordinator.memoryRequested') ? cluster.stats.coordinator.memoryRequested.replace('.00','') : '-' }} / {{ hasProp(cluster,'stats.shards.memoryRequested') ? cluster.stats.shards.memoryRequested.replace('.00','') : '-' }}</span>
										</router-link>
									</td>
									<td class="volumeSize textRight hasTooltip">
										<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" title="Cluster Status" class="noColor">
											<span>{{ cluster.data.spec.coordinator.pods.persistentVolume.size }} / {{ cluster.data.spec.shards.pods.persistentVolume.size }}</span>
										</router-link>
									</td>
									<td class="actions">
										<router-link :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name" target="_blank" class="newTab"></router-link>
										<router-link v-if="iCan('patch','sgshardedclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgshardedcluster/' + cluster.name + '/edit'" title="Edit Cluster" data-active=".set.clu" class="editCRD"></router-link>
										<a v-if="iCan('create','sgshardedclusters',$route.params.namespace)" @click="cloneCRD('SGShardedClusters', $route.params.namespace, cluster.name)" class="cloneCRD" title="Clone Cluster"></a>
										<a v-if="iCan('delete','sgshardedclusters',$route.params.namespace)" @click="deleteCRD('sgshardedclusters', $route.params.namespace, cluster.name)" title="Delete Cluster" class="deleteCRD"></a>
									</td>
								</tr>
							</template>
						</template>
					</template>
				</tbody>
			</table>
			<v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < sgshardedclusters.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="sgshardedclusters.length" @page-change="pageChange" align="center" ref="page"></v-page>
		</div>
		<div id="nameTooltip">
			<div class="info"></div>
		</div>
	</div>
</template>

<script>
	import store from '@/store'
	import { mixin } from '@/components/mixins/mixin'

    export default {
        name: 'ShardedClusterOverview',

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
			sgshardedclusters () {
				return this.sortTable([...(store.state.sgshardedclusters.filter(cluster => (cluster.data.metadata.namespace == this.$route.params.namespace)))], this.currentSort.param, this.currentSortDir, this.currentSort.type)
			},

			profiles () {
				return store.state.sginstanceprofiles
			},			
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
		width: 120px !important;
		min-width: 120px;
		max-width: 120px !important;
	}

	a.cloneCRD svg {
		position: relative;
		top: 1px;
	}

	td.actions a {
		top: 7px;
	}
</style>
