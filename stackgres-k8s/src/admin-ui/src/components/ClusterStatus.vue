<template>
	<div id="cluster-status" v-if="iCanLoad">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">			
			<div class="content noScroll" v-if="hasProp(cluster, 'status.pods') && cluster.status.pods.length">
				<h2>
					Cluster
					<template v-if="hasProp(cluster, 'data.status.confitions')">
						<template v-for="condition in cluster.data.status.conditions" v-if="( (condition.type == 'PendingRestart') && (condition.status == 'True') )">
							<span class="helpTooltip alert" data-tooltip="A restart operation is pending for this cluster"></span>
						</template>
					</template>
				</h2>
				
				<div class="connectionInfo" v-if="hasProp(cluster, 'data.info')" :set="hasPrimary = ( typeof ( cluster.data.pods.find(p => (p.role == 'primary')) ) == 'undefined' )">
					<a @click="setContentTooltip('#connectionInfo', hasPrimary)"> 
						<h2>View Connection Info</h2>
						<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
					</a>

					<div id="connectionInfo" class="hidden">
						<div class="connInfo">
							<div class="textCenter" v-if="( typeof ( cluster.data.pods.find(p => (p.role == 'primary')) ) == 'undefined' )">
								<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 18"><g transform="translate(0 -183)"><path d="M18.994,201H1.006a1,1,0,0,1-.871-.516,1.052,1.052,0,0,1,0-1.031l8.993-15.974a1.033,1.033,0,0,1,1.744,0l8.993,15.974a1.052,1.052,0,0,1,0,1.031A1,1,0,0,1,18.994,201ZM2.75,198.937h14.5L10,186.059Z"/><rect width="2" height="5.378" rx="0.947" transform="translate(9 189.059)"/><rect width="2" height="2" rx="1" transform="translate(9 195.437)"/></g></svg>
								<h4>Attention</h4>
								<p>
									No pods are available yet for this cluster.<br/>
									You won't be able to connect to it until there's at least one active pod.
								</p>
							</div>
							<template v-else>
								<p>To access StackGres cluster <code>{{ $route.params.namespace + '.' + cluster.name }}</code> you can address one or both of the following DNS entries:
									<ul>
										<li>Read Write DNS: <code>{{ cluster.data.info.primaryDns }}</code> </li>
										<li>Read Only DNS: <code>{{ cluster.data.info.replicasDns }}</code> </li>
									</ul>
								</p>	

								<p>You may connect with Postgres client <code>psql</code> in two different ways:
									<ul>
										<li>
											Local <code>psql</code> (runs within the same pod as Postgres):<br/>
											<template v-for="pod in cluster.data.pods">
												<pre v-if="pod.role == 'primary'">kubectl -n {{ $route.params.namespace }} exec -ti {{ pod.name }} -c postgres-util -- psql{{ cluster.data.info.superuserUsername !== 'postgres' ? ' -U '+cluster.data.info.superuserUsername : '' }}<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
											</template>
										</li>
										<li>
											Externally to StackGres pods, from a container image that contains <code>psql</code> (this option is the only one available if you have disabled the <code>postgres-util</code> sidecar):<br/>
											<pre>kubectl -n {{ $route.params.namespace }} run psql --rm -it --image ongres/postgres-util --restart=Never -- psql -h {{ cluster.data.info.primaryDns }} {{ cluster.data.info.superuserUsername }} {{ cluster.data.info.superuserUsername }}  <span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
										</li>
									</ul>
								</p>

								<p>The command will ask for the admin user password (prompt may not be shown, just type or paste the password). Use the following command to retrieve it:<br/>
									<pre>kubectl -n {{ $route.params.namespace }} get secret {{ cluster.data.info.superuserSecretName }} --template <template v-pre>'{{</template> printf "%s" (index .data "{{ cluster.data.info.superuserPasswordKey }}" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
								</p>

								<template v-if="cluster.data.spec.postgres.flavor == 'babelfish'">
									<hr/>

									<p>If you wish to connect via the SQL Server protocol, please use the following command:</p>
									<pre>kubectl -n {{ cluster.data.metadata.namespace }} run usql --rm -it --image ongres/postgres-util --restart=Never -- usql --password ms://babelfish@{{ cluster.data.metadata.name }}:1433<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>

									<br/><br/>
									<p>To retrieve the secrete, please use:</p>
									<pre>kubectl -n {{ cluster.data.metadata.namespace }} get secret {{ cluster.data.metadata.name }} --template '<template v-pre>{{</template> printf "%s" (index .data "babelfish-password" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
								</template>
							</template>
						</div>
					</div>
				</div>

				<table class="clusterInfo fullWidth">
					<thead>
						<th>
							Total CPU 
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.cpuRequested').slice(0, -2) + ' (' + (cluster.status.hasOwnProperty('cpuPsiAvg60') ? getTooltip('sgcluster.pods.cpuPsiAvg60') : getTooltip('sgcluster.pods.averageLoad1m')) + ')'"></span>
						</th>
						<th class="textRight">
							Total Memory
							<span class="helpTooltip" :data-tooltip="cluster.status.hasOwnProperty('memoryPsiAvg60') ? getTooltip('sgcluster.pods.memoryPsiAvg60') : getTooltip('sgcluster.pods.memoryRequested')"></span>
						</th>
						<th class="textRight">
							Primary Node Disk
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.diskUsed').slice(0, -2) + ' / ' + getTooltip('sgcluster.spec.pods.persistentVolume.size') + (cluster.status.hasOwnProperty('diskPsiAvg60') ? ' (' + getTooltip('sgcluster.pods.diskPsiAvg60') + ')' : '')"></span>
						</th>
						<th class="textRight">
							Total Allocated Disk
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterstats.diskRequested')"></span>
						</th>
						<th class="textRight">
							Instances
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.podsReady').slice(0, -2) + ' / ' + getTooltip('sgcluster.spec.instances')"></span>
						</th>
					</thead>
					<tbody>
						<tr>
							<td>
								{{ cluster.status.cpuRequested }} 
								<template v-if="cluster.status.podsReady">
									(avg. load {{ cluster.status.hasOwnProperty('cpuPsiAvg60') ? cluster.status.cpuPsiAvg60 : cluster.status.averageLoad1m }})
								</template>
							</td>
							<td class="textRight">
								{{ cluster.status.hasOwnProperty('memoryPsiAvg60') ? cluster.status.memoryPsiAvg60 : cluster.status.memoryRequested}}
							</td>
							<td class="flex-center">
								<template v-if="cluster.status.hasOwnProperty('pods') && (typeof (cluster.status.pods.find(p => (p.role == 'primary'))) !== 'undefined')">
									<template v-for="pod in cluster.status.pods" v-if="pod.role == 'primary'">
											<div class="donut">
											<svg class="loader" xmlns="http://www.w3.org/2000/svg" version="1.1">
												<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" :stroke-dasharray="diskUsed+',63'" />
											</svg>
											<svg class="background" xmlns="http://www.w3.org/2000/svg" version="1.1">
												<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" />
											</svg>
										</div>
										{{ pod.diskUsed }} / {{ pod.diskRequested }}
									</template>
								</template>
								<template v-else>
									-
								</template>
							</td>
							<td class="textRight">{{ cluster.status.hasOwnProperty('diskRequested') ? cluster.status.diskRequested : '-' }}</td>
							<td class="textRight">{{ cluster.data.podsReady }} / {{ cluster.data.spec.instances }}</td>
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
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.cpuRequested').slice(0, -2) + ' (' + (cluster.status.hasOwnProperty('cpuPsiAvg60') ? getTooltip('sgcluster.pods.cpuPsiAvg60') : getTooltip('sgcluster.pods.averageLoad1m')) + ')'"></span>
						</th>
						<th class="textRight">
							Memory
							<span class="helpTooltip" :data-tooltip="cluster.status.hasOwnProperty('memoryPsiAvg60') ? getTooltip('sgcluster.pods.memoryPsiAvg60') : getTooltip('sgcluster.pods.memoryRequested')"></span>
						</th>
						<th class="textRight">
							Disk
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.diskUsed').slice(0, -2) + ' / ' + getTooltip('sgcluster.pods.diskRequested') + (cluster.status.hasOwnProperty('diskPsiAvg60') ? ' (' + getTooltip('sgcluster.pods.diskPsiAvg60') + ')' : '')"></span>
						</th>
						<th class="textRight">
							Containers
							<span class="helpTooltip" :data-tooltip="getTooltip('sgcluster.pods.containersReady').slice(0, -2) + ' / ' + getTooltip('sgcluster.pods.containers')"></span>
						</th>
					</thead>
					<tbody>
						<tr v-for="pod in cluster.status.pods">
							<td>{{ pod.name }}</td>
							<td class="tag" :class="pod.role"><span>{{ pod.role }}</span></td>
							<td class="tag" :class="pod.status">
								<span class="onHover" :data-tooltip="(pod.status == 'Pending') ? getPodLastEvent(pod.name) : ''" :title="( (pod.status == 'Pending') && getPodLastEvent(pod.name).length) ? 'Click for details' : ''">
									{{ pod.status }}
								</span>
							</td>
							<td>
								{{ pod.cpuRequested }} 
								<template v-if="pod.status !== 'Pending'">
									(avg. load {{ pod.hasOwnProperty('cpuPsiAvg60') ? pod.cpuPsiAvg60 : pod.averageLoad1m }})
								</template>
								
								<!--Temporarily disabled until REST API response data is set properly-->
									
								<!--<template v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
									<template v-if="( pod.cpuRequested != ( (pod.cpuRequested.includes('m') && !profile.data.spec.cpu.includes('m')) ? ( (profile.data.spec.cpu * 1000) + 'm') : profile.data.spec.cpu ) )">
										<span class="helpTooltip alert" data-tooltip="A CPU change request is pending to be applied"></span>
									</template>
								</template>-->
							</td>
							<td class="textRight">
								{{ pod.hasOwnProperty('memoryPsiAvg60') ? pod.memoryPsiAvg60 : pod.memoryRequested }}
								
								<!--Temporarily disabled until REST API response data is set properly-->
									
								<!--<template v-for="profile in profiles" v-if="( (profile.name == cluster.data.spec.sgInstanceProfile) && (profile.data.metadata.namespace == cluster.data.metadata.namespace) )">
									<template v-if="( (pod.hasOwnProperty('memoryPsiAvg60') ? pod.memoryPsiAvg60 : pod.memoryRequested).replace('.00','') != profile.data.spec.memory) ">
										<span class="helpTooltip alert" data-tooltip="A memory change request is pending to be applied"></span>
									</template>
								</template>-->
							</td>
							<td class="textRight">
							<template v-if="pod.hasOwnProperty('diskUsed')">{{ pod.diskUsed }}</template><template v-else>-</template> / {{ pod.diskRequested }} <span v-if="pod.hasOwnProperty('diskPsiAvg60')">(psi avg. {{ pod.diskPsiAvg60 }})</span>
							</td>
							<td class="textRight">{{ pod.containersReady }} / {{ pod.containers }}</td>
						</tr>
					</tbody>
				</table>
			</div>
			<div class="no-data" v-else-if="hasProp(cluster, 'status.pods') && !cluster.status.pods.length">
				No pods yet available
			</div>
			<div class="no-data" v-else>
				Loading cluster status...
			</div>
		</template>
	</div>
</template>

<script>
	import store from '../store'
	import sgApi from '../api/sgApi'
	import { mixin } from './mixins/mixin'

    export default {
        name: 'ClusterStatus',

		mixins: [mixin],

		data: function() {
			return {
				events: [],
				eventsPooling: null
			}
		},
		
		mounted: function() {
			const vc = this;

			vc.getClusterEvents();
			vc.eventsPooling = setInterval( function() {
				vc.getClusterEvents()
			}, 10000);
		},
		
		computed: {

			clusters () {
				return store.state.sgclusters
			},

			pods () {
				return store.state.currentPods
			},

			diskUsed () {
				const vc = this
				
				if( store.state.currentCluster.hasOwnProperty('status') && store.state.currentCluster.status.hasOwnProperty('pods')) {
					let primary = store.state.currentCluster.status.pods.find(p => (p.role == 'primary'))
					
					if(typeof primary != 'undefined') {
						let used = vc.getBytes(primary.diskUsed);
						let available = vc.getBytes(store.state.currentCluster.data.spec.pods.persistentVolume.size);
						let percentage = Math.round((used*63)/available);
						
						return percentage
					} else {
						return 0
					}

				} else {
					return 0
				}

			},
			
			tooltips () {
				return store.state.tooltips
			},

			profiles () {
				return store.state.sginstanceprofiles
			}

		},

		methods: {
			getClusterEvents() {
				const vc = this;
				
				sgApi
				.getResourceDetails('sgclusters', vc.$route.params.namespace, vc.$route.params.name, 'events')
				.then( function(response) {
					vc.events = [...response.data]
				}).catch(function(err) {
					console.log(err);
					vc.checkAuthError(err);
				});
			},

			getPodLastEvent(podName) {
				const vc = this;

				let event = vc.events.find(e => ( ( e.involvedObject.kind == 'Pod' ) && (e.involvedObject.name == podName) ) )

				if(event !== undefined)
					return event.message
				else
					return 'There are no events related to this pod'
			}
		},

		beforeDestroy () {
			clearInterval(this.eventsPooling);
		} 
	}
</script>

<style scoped>
	table.podStatus td {
		position: relative;
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

	.podStatus td.tag.Pending span {
		cursor: pointer;
	}

	.podStatus td.tag.Pending span:after {
		content: " ";
		display: inline-block;
		position: absolute;
		width: 15px;
		height: 15px;
		background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyMCAyMCI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0iTTE5IDE5SDFjLS40IDAtLjctLjItLjktLjUtLjItLjMtLjItLjcgMC0xbDktMTZjLjMtLjUuOS0uNiAxLjQtLjNsLjMuMyA5IDE2Yy4yLjMuMi43IDAgMS0uMS4zLS40LjUtLjguNXpNMi44IDE2LjloMTQuNUwxMCA0LjEgMi44IDE2Ljl6Ii8+PHBhdGggZD0iTTkuOSA3LjFoLjFjLjYgMCAxIC40IDEgLjl2My41YzAgLjUtLjQuOS0uOS45aC0uMmMtLjUgMC0uOS0uNC0uOS0uOVY4YzAtLjUuNC0uOS45LS45ek0xMCAxMy40Yy42IDAgMSAuNCAxIDFzLS40IDEtMSAxLTEtLjQtMS0xYzAtLjUuNC0xIDEtMXoiLz48L2c+PC9zdmc+) center no-repeat;
		transform: translateX(15px);
		background-size: contain;
		margin-left: 5px;
	}

	.flex-center {
		justify-content: flex-end;
	}
</style>	