var ClusterStatus = Vue.component("ClusterStatus", {
	template: `
		<div id="cluster-status">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">{{ currentNamespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<router-link :to="'/admin/overview/'+currentNamespace" title="Namespace Overview">SGClusters</router-link>
					</li>
					<li>
						<router-link :to="'/admin/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status">{{ $route.params.name }}</router-link>
					</li>
					<li>
						Status
					</li>
				</ul>

				<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/04-postgres-cluster-management/01-postgres-clusters/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div>
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', currentNamespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/admin/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" v-on:click="deleteCRD('sgcluster', currentNamespace, $route.params.name, '/admin/overview/'+currentNamespace)" :class="'/overview/'+currentNamespace">Delete Cluster</a>
					</div>
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/admin/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/admin/cluster/configuration/'+$route.params.namespace+'/'+$route.params.name" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="iCan('list','sgbackups',$route.params.namespace)">
						<router-link :to="'/admin/cluster/backups/'+$route.params.namespace+'/'+$route.params.name" title="Backups" class="backups">Backups</router-link>
					</li>
					<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace) && cluster.data.spec.hasOwnProperty('distributedLogs')">
						<router-link :to="'/admin/cluster/logs/'+$route.params.namespace+'/'+$route.params.name" title="Distributed Logs" class="logs">Logs</router-link>
					</li>
					<li v-if="cluster.data.grafanaEmbedded">
						<router-link id="grafana-btn" :to="'/admin/cluster/monitor/'+$route.params.namespace+'/'+$route.params.name" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
				</ul>
			</header>

			<div class="content">
				<h2>Cluster</h2>
				<table class="clusterInfo">
					<thead>
						<th>
							Total CPU 
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Total Memory
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Primary Node Disk
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Instances
							<span class="helpTooltip" @hover=""></span>
						</th>
					</thead>
					<tbody>
						<tr>
							<td v-if="cluster.status.hasOwnProperty('cpuPsiAvg60')">
								{{ cluster.status.cpuRequested }} (avg. load {{ cluster.status.cpuPsiAvg60 }})
							</td>
							<td v-else>
								{{ cluster.status.cpuRequested }} (avg. load {{ cluster.status.averageLoad1m }})
							</td>
							<td v-if="cluster.status.hasOwnProperty('memoryPsiAvg60')">
								{{ cluster.status.memoryPsiAvg60 }}
							</td>
							<td>
								{{ cluster.status.memoryRequested }}
							</td>
							<td class="flex-center">
								<div class="donut">
									<svg class="loader" xmlns="http://www.w3.org/2000/svg" version="1.1">
										<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" :stroke-dasharray="diskUsed+',63'" />
									</svg>
									<svg class="background" xmlns="http://www.w3.org/2000/svg" version="1.1">
										<circle cx="12.5" cy="12.5" r="10" stroke-width="5" fill="none" />
									</svg>
								</div>
								{{ cluster.status.diskUsed }} / {{ cluster.data.spec.pods.persistentVolume.size }} <span v-if="cluster.status.hasOwnProperty('ioPsiAvg60')">(psi avg. {{ cluster.status.ioPsiAvg60 }})</span>
							</td>
							<td>{{ cluster.data.podsReady }} / {{ cluster.data.pods.length }}</td>
						</tr>
					</tbody>
				</table>

				<h2>Pods</h2>
				<table class="podStatus">
					<thead>
						<th>
							Pod Name
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Role
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Status
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							CPU
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Memory
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Disk
							<span class="helpTooltip" @hover=""></span>
						</th>
						<th>
							Containers
							<span class="helpTooltip" @hover=""></span>
						</th>
					</thead>
					<tbody>
						<tr v-for="pod in cluster.status.pods">
							<td>{{ pod.name }}</td>
							<td class="label" :class="pod.role"><span>{{ pod.role }}</span></td>
							<td class="label" :class="pod.status"><span>{{ pod.status }}</span></td>
							<td v-if="pod.hasOwnProperty('cpuPsiAvg60')">
								{{ pod.cpuRequested }} (avg. load {{ pod.cpuPsiAvg60 }})
							</td>
							<td v-else>
								{{ pod.cpuRequested }} (avg. load {{ pod.averageLoad1m }})
							</td>
							<td v-if="pod.hasOwnProperty('memoryPsiAvg60')">
								{{ pod.memoryPsiAvg60 }}
							</td>
							<td>
								{{ pod.memoryRequested }}
							</td>
							<td>
								{{ pod.diskUsed }} / {{ pod.diskRequested }} <span v-if="pod.hasOwnProperty('ioPsiAvg60')">(psi avg. {{ pod.ioPsiAvg60 }})</span>
							</td>
							<td>{{ pod.containersReady }} / {{ pod.containers }}</td>
						</tr>
					</tbody>
				</table>
			</div>
		</template>
		</div>`,
	data: function() {
		return {
			//dataReady: [ false, false ],
			//allDataReady: false,
			//polling: null,
			name: '',
			namespace: ''
	    }
	},
	methods: {
		
		fetchAPI: function() {
			vc = this;

		},

		deleteCluster: function(e) {
			//e.preventDefault();

			let confirmDelete = confirm("DELETE ITEM\nAre you sure you want to delete this item?")

			if(confirmDelete) {
				const cl = {
					name: this.name,
					namespace: this.namespace
				}
	
				const res = axios
				.delete(
					apiURL+'sgcluster/', 
					{
						data: {
							"metadata": {
								"name": cl.name,
								"namespace": cl.namespace
							}
						}
					}
				)
				.then(function (response) {
					console.log("DELETED");
					//console.log(response);
					notify('Cluster <strong>'+this.$route.params.name+'</strong> deleted successfully', 'message');
					this.fetchAPI();
					router.push('/admin/overview/'+store.state.currentNamespace);                        
				})
				.catch(function (error) {
					console.log(error.response);
					notify(error.response.data.message,'error');
				});
			}

		}	

	},
	mounted: function() {


	},
	created: function() {

		if ( (store.state.currentCluster.length > 0) && (store.state.currentCluster.name == this.$route.params.name) ) {
			this.dataReady = true;
		}

		this.name = this.$route.params.name;
		this.namespace = this.$route.params.namespace;
		
	},
	computed: {

		clusters () {
			//console.log(store.state.currentCluster);
			//return store.state.currentCluster

			return store.state.clusters
		},
		pods () {
			//console.log(store.state.currentPods);
			return store.state.currentPods
		},
		currentNamespace () {
			return store.state.currentNamespace
		},
		diskUsed () {
			
			if( store.state.currentCluster.hasOwnProperty('status') && store.state.currentCluster.status.hasOwnProperty('diskUsed') ) {
				let used = getBytes(store.state.currentCluster.status.diskUsed);
				let available = getBytes(store.state.currentCluster.data.spec.pods.persistentVolume.size);
				let percentage = Math.round((used*63)/available);

				return percentage
			} else
				return 0

		},
		notFound () {
			//window.location.href = '/admin/not-found.html';
		}
	},
	beforeDestroy () {
		clearInterval(this.polling);
		//console.log('Interval cleared');
	} 
})