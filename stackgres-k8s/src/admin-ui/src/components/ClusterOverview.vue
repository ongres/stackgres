<template>
	<div id="cluster-overview" v-if="loggedIn && isReady && !notFound">
		<template v-for="namespace in namespaces" v-if="(namespace == $route.params.namespace)">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						SGClusterList
					</li>
				</ul>

				<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div>
						<router-link :to="'/' + $route.params.namespace + '/sgclusters/new'" class="add" v-if="iCan('create','sgclusters',$route.params.namespace)">Add New</router-link>
					</div>	
				</div>	
			</header>

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
											<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + cluster.name + '/edit'" title="Edit Cluster" data-active=".set.clu" class="editCRD"></router-link>
											<a v-if="iCan('create','sgclusters',$route.params.namespace)" @click="cloneCRD('SGClusters', $route.params.namespace, cluster.name)" class="cloneCRD" title="Clone Cluster"></a>
											<a @click="setRestartCluster($route.params.namespace, cluster.name)" class="restartCluster" title="Restart Cluster"></a>
											<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgclusters', $route.params.namespace, cluster.name)" title="Delete Cluster" class="deleteCRD"></a>
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
