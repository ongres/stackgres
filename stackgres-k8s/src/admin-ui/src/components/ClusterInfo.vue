<template>
	<div id="cluster-info" v-if="loggedIn && isReady && !notFound">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
			<header>
				<ul class="breadcrumbs">
					<li class="namespace">
						<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
						<router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
					</li>
					<li>
						<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
						<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="SGClusters">SGClusters</router-link>
					</li>
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name" title="Status">{{ $route.params.name }}</router-link>
					</li>
					<li>
						Configuration
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div class="crdActionLinks">
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGClusters', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/edit'">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgclusters', $route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/sgclusters')" class="deleteCRD" :class="$route.params.namespace + '/sgclusters'">Delete Cluster</a>
						<a @click="setRestartCluster($route.params.namespace, $route.params.name)" class="restartCluster" title="Restart Cluster">Restart Cluster</a>
					</div>		
				</div>

				<ul class="tabs">
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name" title="Status" class="status">Status</router-link>
					</li>
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/config'" title="Configuration" class="info">Configuration</router-link>
					</li>
					<li v-if="iCan('list','sgbackups',$route.params.namespace)">
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/sgbackups'" title="Backups" class="backups">Backups</router-link>
					</li>
					<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace) && cluster.data.spec.hasOwnProperty('distributedLogs')">
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/logs'" title="Distributed Logs" class="logs">Logs</router-link>
					</li>
					<li v-if="cluster.data.grafanaEmbedded">
						<router-link id="grafana-btn" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/monitor'" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
					</li>
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/events'" title="Events" class="events">Events</router-link>
					</li>
				</ul>
			</header>

			<div class="content noScroll">
				<h2>Cluster Details</h2>
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
											<pre>kubectl -n {{ $route.params.namespace }} run psql --rm -it --image ongres/postgres-util --restart=Never -- psql -h {{ cluster.name }}-primary {{ cluster.data.info.superuserUsername }} {{ cluster.data.info.superuserUsername }}  <span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
										</li>
									</ul>
								</p>

								<p>The command will ask for the admin user password (prompt may not be shown, just type or paste the password). Use the following command to retrieve it:<br/>
									<pre>kubectl -n {{ $route.params.namespace }} get secret {{ cluster.data.info.superuserSecretName }} --template <template v-pre>'{{</template> printf "%s" (index .data "{{ cluster.data.info.superuserPasswordKey }}" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
								</p>
							</template>
						</div>
					</div>
				</div>
				
				<ClusterDetails :cluster="cluster" :extensionsList="extensionsList"></ClusterDetails>
			</div>
		</template>
	</div>
</template>

<script>
	import store from '../store'
	import axios from 'axios'
	import { mixin } from './mixins/mixin'
	import ClusterDetails from './details/ClusterDetails.vue'
	
    export default {
        name: 'ClusterInfo',

		mixins: [mixin],

		components: {
			ClusterDetails
		},

		data() {
			return {
				extensionsList: [],
			}
		},

		computed: {

			clusters () {
				return store.state.clusters
			},

		},

		created() {
			const vc = this;

			if(!vc.extensionsList.length) {

				axios
				.get('/stackgres/extensions/latest')
				.then(function (response) {
					vc.extensionsList = response.data.extensions
				})
				.catch(function (error) {
					console.log(error.response);
					vc.notify(error.response.data,'error','sgclusters');
				});

			}
		}

	}
</script>

<style>
	.helpTooltip.alert {
		position: absolute;
		right: 30px;
		top: 11px;
	}

	table.clusterConfig a > svg {
		margin-top: 3px;
	}
</style>