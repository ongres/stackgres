<template>
	<div id="cluster-status" v-if="iCanLoad">
		<div class="content noScroll" v-if="hasClusters && hasPods">
			<h2>
				Cluster
				<template v-if="hasProp(sgshardedcluster, 'data.status.conditions')">
					<template v-for="condition in sgshardedcluster.data.status.conditions" v-if="( (condition.type == 'PendingRestart') && (condition.status == 'True') )">
						<span class="helpTooltip alert" data-tooltip="A restart operation is pending for this cluster"></span>
					</template>
				</template>
			</h2>
			
			<div class="connectionInfo" v-if="hasProp(sgshardedcluster, 'data.info')">
				<a @click="setContentTooltip('#connectionInfo', !podsReady)"> 
					<h2>View Connection Info</h2>
					<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
				</a>

				<div id="connectionInfo" class="hidden">
					<div class="connInfo">
						<div class="textCenter" v-if="!podsReady">
							<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 18"><g transform="translate(0 -183)"><path d="M18.994,201H1.006a1,1,0,0,1-.871-.516,1.052,1.052,0,0,1,0-1.031l8.993-15.974a1.033,1.033,0,0,1,1.744,0l8.993,15.974a1.052,1.052,0,0,1,0,1.031A1,1,0,0,1,18.994,201ZM2.75,198.937h14.5L10,186.059Z"/><rect width="2" height="5.378" rx="0.947" transform="translate(9 189.059)"/><rect width="2" height="2" rx="1" transform="translate(9 195.437)"/></g></svg>
							<h4>Attention</h4>
							<p>
								No pods are available yet for this cluster.<br/>
								You won't be able to connect to it until there's at least one active pod.
							</p>
						</div>
						<template v-else>
							<p>To access StackGres cluster <code>{{ $route.params.namespace + '.' + sgshardedcluster.name }}</code> you can address one or both of the following DNS entries:
								<ul>
									<li>Read Write DNS: <code>{{ sgshardedcluster.data.info.primaryDns }}</code> </li>
									<li>Read Only DNS: <code>{{ sgshardedcluster.data.info.readsDns }}</code> </li>
								</ul>
							</p>	

							<p>You may connect with Postgres client <code>psql</code> in two different ways:
								<ul>
									<li>
										Local <code>psql</code> (runs within the same pod as Postgres):<br/>
										<pre>kubectl -n {{ $route.params.namespace }} exec -ti {{ sgshardedcluster.data.status.clusters[0] }} -c postgres-util -- psql{{ sgshardedcluster.data.info.superuserUsername !== 'postgres' ? ' -U '+ sgshardedcluster.data.info.superuserUsername : '' }}<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
									</li>
									<li>
										Externally to StackGres pods, from a container image that contains <code>psql</code> (this option is the only one available if you have disabled the <code>postgres-util</code> sidecar):<br/>
										<pre>kubectl -n {{ $route.params.namespace }} run psql --rm -it --image ongres/postgres-util --restart=Never -- psql -h {{ sgshardedcluster.data.info.primaryDns }} {{ sgshardedcluster.data.info.superuserUsername }} {{ sgshardedcluster.data.info.superuserUsername }}  <span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
									</li>
								</ul>
							</p>

							<p>The command will ask for the admin user password (prompt may not be shown, just type or paste the password). Use the following command to retrieve it:<br/>
								<pre>kubectl -n {{ $route.params.namespace }} get secret {{ sgshardedcluster.data.info.superuserSecretName }} --template <template v-pre>'{{</template> printf "%s" (index .data "{{ sgshardedcluster.data.info.superuserPasswordKey }}" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
							</p>

							<template v-if="sgshardedcluster.data.spec.postgres.flavor == 'babelfish'">
								<hr/>

								<p>If you wish to connect via the SQL Server protocol, please use the following command:</p>
								<pre>kubectl -n {{ sgshardedcluster.data.metadata.namespace }} run usql --rm -it --image ongres/postgres-util --restart=Never -- usql --password ms://babelfish@{{ sgshardedcluster.data.metadata.name }}:1433<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>

								<br/><br/>
								<p>To retrieve the secrete, please use:</p>
								<pre>kubectl -n {{ sgshardedcluster.data.metadata.namespace }} get secret {{ sgshardedcluster.data.metadata.name }} --template '<template v-pre>{{</template> printf "%s" (index .data "babelfish-password" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
							</template>
						</template>
					</div>
				</div>
			</div>
			<template v-if="Object.keys(sgshardedcluster.stats).length">
				<table class="clusterInfo fullWidth">
					<thead>
						<th>
							Total CPU 
							<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.stats.coordinator.cpuRequested').slice(0, -2)"></span>
						</th>
						<th class="textRight">
							Total Memory
							<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.stats.coordinator.memoryRequested')"></span>
						</th>
						<th class="textRight">
							Primary Node Disk
							<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.stats.coordinator.diskUsed').slice(0, -2) + ' / ' + getTooltip('sgshardedcluster.spec.coordinator.pods.persistentVolume.size')"></span>
						</th>
						<th class="textRight">
							Total Allocated Disk
							<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.stats.coordinator.diskRequested')"></span>
						</th>
						<th class="textRight">
							Instances
							<span class="helpTooltip" :data-tooltip="getTooltip('sgshardedcluster.stats.coordinator.podsReady') + ' / ' + getTooltip('sgshardedcluster.spec.coordinator.instances') + ' – ' + getTooltip('sgshardedcluster.stats.shards.podsReady') + ' / ' + getTooltip('sgshardedcluster.spec.shards.instancesPerCluster')"></span>
						</th>
					</thead>
					<tbody>
						<tr>
							<td>
								{{ sgshardedcluster.stats.coordinator.cpuRequested }} 
								<template v-if="sgshardedcluster.stats.hasOwnProperty('cpuPsiAvg60') || sgshardedcluster.stats.hasOwnProperty('averageLoad1m')">
									(avg. load {{ sgshardedcluster.stats.hasOwnProperty('cpuPsiAvg60') ? sgshardedcluster.stats.cpuPsiAvg60 : sgshardedcluster.stats.averageLoad1m }})
								</template>
							</td>
							<td class="textRight">
								{{ sgshardedcluster.stats.coordinator.hasOwnProperty('memoryPsiAvg60') ? sgshardedcluster.stats.coordinator.memoryPsiAvg60 : sgshardedcluster.stats.coordinator.memoryRequested }}
							</td>
							<td class="flex-center">
								<div class="donut">
									<svg class="loader" xmlns="http://www.w3.org/2000/svg" version="1.1">
										<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" :stroke-dasharray="diskUsed(sgshardedcluster.stats.coordinator)+',63'" />
									</svg>
									<svg class="background" xmlns="http://www.w3.org/2000/svg" version="1.1">
										<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" />
									</svg>
								</div>
								{{ sgshardedcluster.stats.coordinator.hasOwnProperty('diskUsed') ? sgshardedcluster.stats.coordinator.diskUsed : '-' }} / {{ sgshardedcluster.stats.coordinator.diskRequested }}
							</td>
							<td class="textRight">
								{{ sgshardedcluster.stats.coordinator.diskRequested }}
							</td>
							<td class="textRight">
								{{ sgshardedcluster.stats.coordinator.podsReady }} / {{ sgshardedcluster.data.spec.coordinator.instances }}
							</td>
						</tr>
						<tr>
							<td>
								{{ sgshardedcluster.stats.shards.cpuRequested }} 
								<template v-if="sgshardedcluster.stats.hasOwnProperty('cpuPsiAvg60') || sgshardedcluster.stats.hasOwnProperty('averageLoad1m')">
									(avg. load {{ sgshardedcluster.stats.hasOwnProperty('cpuPsiAvg60') ? sgshardedcluster.stats.cpuPsiAvg60 : sgshardedcluster.stats.averageLoad1m }})
								</template>
							</td>
							<td class="textRight">
								{{ sgshardedcluster.stats.shards.hasOwnProperty('memoryPsiAvg60') ? sgshardedcluster.stats.shards.memoryPsiAvg60 : sgshardedcluster.stats.shards.memoryRequested }}
							</td>
							<td class="flex-center">
								<div class="donut">
									<svg class="loader" xmlns="http://www.w3.org/2000/svg" version="1.1">
										<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" :stroke-dasharray="diskUsed(sgshardedcluster.stats.shards)+',63'" />
									</svg>
									<svg class="background" xmlns="http://www.w3.org/2000/svg" version="1.1">
										<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" />
									</svg>
								</div>
								{{ sgshardedcluster.stats.shards.hasOwnProperty('diskUsed') ? sgshardedcluster.stats.shards.diskUsed : '-' }} / {{ sgshardedcluster.stats.shards.diskRequested }}
							</td>
							<td class="textRight">
								{{ sgshardedcluster.stats.shards.diskRequested }}
							</td>
							<td class="textRight">
								{{ sgshardedcluster.stats.shards.podsReady }} / {{ sgshardedcluster.data.spec.shards.instancesPerCluster }}
							</td>
						</tr>
					</tbody>
				</table>

				<h2>Pods</h2>
				<table class="podStatus fullWidth">
					<thead>
						<th>
							Pod Name
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.name')"></span>
						</th>
						<th>
							Role
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.role')"></span>
						</th>
						<th>
							Status
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.status')"></span>
						</th>
						<th>
							CPU
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.cpuRequested').slice(0, -2)"></span>
						</th>
						<th class="textRight">
							Memory
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.memoryRequested')"></span>
						</th>
						<th class="textRight">
							Disk
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.diskUsed').slice(0, -2) + ' / ' + getTooltip('sgcluster.pods.diskRequested')"></span>
						</th>
						<th class="textRight">
							Containers
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.containersReady').slice(0, -2) + ' / ' + getTooltip('sgcluster.pods.containers')"></span>
						</th>
					</thead>
					<tbody>
						<template v-for="(cluster, index) in sgclusters">
							<tr v-for="pod in cluster.status.pods">
								<td>
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										{{ pod.name }}
									</router-link>
								</td>
								<td class="tag" :class="pod.role">
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										<span>
											{{ pod.role }}
										</span>
									</router-link>
								</td>
								<td class="tag" :class="pod.status">
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										<span>
											{{ pod.status }}
										</span>
									</router-link>
								</td>
								<td>
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										{{ pod.cpuRequested }} 
										<template v-if="(pod.status !== 'Pending') && ( pod.hasOwnProperty('cpuPsiAvg60') || pod.hasOwnProperty('averageLoad1m') )">
											(avg. load {{ pod.hasOwnProperty('cpuPsiAvg60') ? pod.cpuPsiAvg60 : pod.averageLoad1m }})
										</template>
									</router-link>
								</td>
								<td class="textRight">
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										{{ pod.hasOwnProperty('memoryPsiAvg60') ? pod.memoryPsiAvg60 : pod.memoryRequested }}
									</router-link>
								</td>
								<td class="textRight">
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										<template v-if="pod.hasOwnProperty('diskUsed')">
											{{ pod.diskUsed }}
										</template>
										<template v-else>
											-
										</template>
										/ {{ pod.diskRequested }} <span v-if="pod.hasOwnProperty('diskPsiAvg60')">(psi avg. {{ pod.diskPsiAvg60 }})</span>
									</router-link>
								</td>
								<td class="textRight">
									<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + getClusterName(pod.name)" title="Cluster Details" class="noColor">
										{{ pod.containersReady }} / {{ pod.containers }}
									</router-link>
								</td>
							</tr>
						</template>
					</tbody>
				</table>
			</template>
			<div class="no-data" v-else>
				Loading cluster status...
			</div>
		</div>
		<div class="no-data" v-else>
			No data yet available
		</div>
	</div>
</template>

<script>
	import store from '@/store'
	import { mixin } from '@/components/mixins/mixin'

    export default {
        name: 'ShardedClusterStatus',

		mixins: [mixin],

		computed: {

			sgshardedcluster () {
				return store.state.sgshardedclusters.find( cluster => (cluster.name == this.$route.params.name) && (cluster.data.metadata.namespace == this.$route.params.namespace) )
			},

			sgclusters () {
				return store.state.sgclusters.filter( cluster => (this.sgshardedcluster.data.status.clusters.includes(cluster.name)) && (cluster.data.metadata.namespace == this.$route.params.namespace) )
			},

			hasClusters() {
				return (this.hasProp(this.sgshardedcluster, 'data.status.clusters') && this.sgshardedcluster.data.status.clusters.length)
			},

			hasPods() {
				return ( (this.hasProp(this.sgshardedcluster, 'stats.coordinator.pods') && this.sgshardedcluster.stats.coordinator.pods.length) || ((this.hasProp(this.sgshardedcluster, 'stats.shards.pods') && this.sgshardedcluster.stats.shards.pods.length) ) )
			},

			podsReady() {
				return (this.hasPods && this.hasProp(this.sgshardedcluster, 'stats.coordinator.podsReady') && this.sgshardedcluster.stats.coordinator.podsReady && this.hasProp(this.sgshardedcluster, 'stats.shards.podsReady') && this.sgshardedcluster.stats.shards.podsReady)
			}

		},

		methods: {
			diskUsed(cluster) {
				const vc = this
				
				if( vc.hasProp(cluster, 'status.pods') ) {
					let primary = cluster.status.pods.find(p => (p.role == 'primary'))
					
					if(typeof primary != 'undefined') {
						let used = vc.getBytes(primary.diskUsed);
						let available = vc.getBytes(cluster.data.spec.pods.persistentVolume.size);
						let percentage = Math.round((used*63)/available);
						
						return percentage
					} else {
						return 0
					}

				} else {
					return 0
				}

			},

			getClusterName(pod) {
				return pod.substring(0, pod.lastIndexOf('-'))
			}
		}
	}
</script>

<style scoped>
	table.podStatus td {
		position: relative;
		padding: 0;
	}

	.podStatus .helpTooltip.alert {
		position: relative;
		top: 2px;
		left: 0px;
		transform: translateX(5px);
	}

	h2 .helpTooltip.alert {
		top: 2px;
	}

	.flex-center {
		justify-content: flex-end;
	}

	td.tag span:not(.helpTooltip) {
		width: 90px;
	}

	.podStatus .noColor {
		width: 100%;
		display: inline-block;
		padding: 10px;
		height: 100%;
	}
</style>	