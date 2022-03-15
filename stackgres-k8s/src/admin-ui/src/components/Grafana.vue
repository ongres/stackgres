<template>
	<div id="grafana" v-if="loggedIn && isReady && !notFound">
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
						Monitoring
					</li>
				</ul>

				<div class="actions">
				<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div class="crdActionLinks">
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGClusters', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/edit'">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgclusters', $route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/sgclusters')" :class="$route.params.namespace + '/sgclusters'">Delete Cluster</a>
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
						<router-link id="grafana-btn" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/monitor'" title="Grafana Dashboard" class="grafana router-link-exact-active">Monitoring</router-link>
					</li>
					<li>
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/events'" title="Events" class="events">Events</router-link>
					</li>
				</ul>

				<template v-if="cluster.data.pods.length && grafanaUrl.length">
					<select class="plain" id="timeRange" v-model="timeRange" @change="goTo('/' + $route.params.namespace + '/sgcluster/' + cluster.name + '/monitor/' + selectedNode + '/' + timeRange)">
						<option disabled value=""><strong>Choose time range</strong></option>
						<option v-for="(time, id) in timeRangeOptions" :value="id">
							{{ time.label }}
						</option>
					</select>

					<select class="plain" v-model="selectedNode" @change="goTo('/' + $route.params.namespace + '/sgcluster/' + cluster.name + '/monitor/' + selectedNode + '/' + timeRange)">
						<option disabled value=""><strong>Choose node</strong></option>
						<option v-for="pod in cluster.data.pods" :value="pod.ip">
							{{ pod.name }}
							<template v-if="pod.role == 'primary'"><span>(primary)</span></template>
						</option>
					</select>
				</template>
			</header>

			<div class="content grafana">
				<template v-if="cluster.data.pods.length">
					<iframe v-if="grafanaUrl.length" :src="(grafanaUrl + (($route.params.hasOwnProperty('pod') && $route.params.pod.length) ? $route.params.pod : selectedNode) + ($route.params.hasOwnProperty('range') ? timeRangeOptions[timeRange].range : ''))" id="grafana"></iframe>
				</template>
				<div v-else class="no-data">
					No pods yet available
				</div>
			</div>
		</template>
	</div>
</template>

<script>
	import router from '../router'
	import store from '../store'
	import { mixin } from './mixins/mixin'


    export default {
        name: 'Grafana',

		mixins: [mixin],

		data: function() {

			return {
				grafanaUrl: '',
				timeRange: this.$route.params.hasOwnProperty('range') ? this.$route.params.range : '',
				timeRangeOptions: {
					'last-5-minutes': { label: 'Last 5 minutes', range: '&from=now-5m&to=now' },
					'last-15-minutes': { label: 'Last 15 minutes', range: '&from=now-15m&to=now' },
					'last-30-minutes': { label: 'Last 30 minutes', range: '&from=now-30m&to=now' },
					'last-1-hour': { label: 'Last 1 hour', range: '&from=now-1h&to=now' },
					'last-3-hours': { label: 'Last 3 hours', range: '&from=now-3h&to=now' },
					'last-6-hours': { label: 'Last 6 hours', range: '&from=now-6h&to=now' },
					'last-12-hours': { label: 'Last 12 hours', range: '&from=now-12h&to=now' },
					'last-24-hours': { label: 'Last 24 hours', range: '&from=now-24h&to=now' },
					'last-2-days': { label: 'Last 2 days', range: '&from=now-2d&to=now' },
					'last-7-days': { label: 'Last 7 days', range: '&from=now-7d&to=now' },
					'last-30-days': { label: 'Last 30 days', range: '&from=now-30d&to=now' },
					'last-90-days': { label: 'Last 90 days', range: '&from=now-90d&to=now' },
					'last-6-months': { label: 'Last 6 months', range: '&from=now-6M&to=now' },
					'last-1-year': { label: 'Last 1 year', range: '&from=now-1y&to=now' },
					'last-3-years': { label: 'Last 3 years', range: '&from=now-3y&to=now' },
					'last-5-years': { label: 'Last 5 years', range: '&from=now-5y&to=now' },
					'yesterday': { label: 'Yesterday', range: '&from=now-1d%2Fd&to=now-1d%2Fd' },
					'day-before-yesterday': { label: 'Day before yesterday', range: '&from=now-2d%2Fd&to=now-2d%2Fd' },
					'this-day-last-week': { label: 'This day last week', range: '&from=now-7d%2Fd&to=now-7d%2Fd' },
					'day-before-yesterday': { label: 'Day before yesterday', range: '&from=now-2d%2Fd&to=now-2d%2Fd' },
					'previous-week': { label: 'Previous week', range: '&from=now-1w%2Fw&to=now-1w%2Fw' },
					'previous-month': { label: 'Previous month', range: '&from=now-1M%2FM&to=now-1M%2FM' },
					'previous-year': { label: 'Previous year', range: '&from=now-1y%2Fy&to=now-1y%2Fy' },
					'today': { label: 'Today', range: '&from=now%2Fd&to=now%2Fd' },
					'today-so-far': { label: 'Today so far', range: '&from=now%2Fd&to=now' },
					'this-week': { label: 'This week', range: '&from=now%2Fw&to=now%2Fw' },
					'this-week-so-far': { label: 'This week so far', range: '&from=now%2Fw&to=now' },
					'this-month': { label: 'This month', range: '&from=now%2FM&to=now%2FM' },
					'this-month-so-far': { label: 'This month so far', range: '&from=now%2FM&to=now' },
					'this-year': { label: 'This year', range: '&from=now%2Fy&to=now%2Fy' },
					'this-year-so-far': { label: 'This year so far', range: '&from=now%2Fy&to=now' }
				},
				selectedNode: this.$route.params.hasOwnProperty('pod') ? this.$route.params.pod : ''
			}
		},
		
		computed: {

			clusters () {
				let vc = this;

				// Read Grafana URL
				if(!vc.$route.params.hasOwnProperty('pod')) {

					let cluster = store.state.clusters.find(c => ((vc.$route.params.namespace == c.data.metadata.namespace) && (vc.$route.params.name == c.name)));
					if(typeof cluster != 'undefined') {
						let primaryNode = cluster.data.pods.find(p => (p.role == 'primary'));

						if(typeof primaryNode != 'undefined') {
							if(vc.$route.path.endsWith('/'))
								router.replace(vc.$route.path + primaryNode.ip)
							else 
								router.replace(vc.$route.path + '/' + primaryNode.ip)
						}
					}
				} else {
					$.get("/grafana")
					.done(function( data, textStatus, jqXHR ) {
						
						if(!data.startsWith('<!DOCTYPE html>')) { // Check "/grafana" isn't just returning web console's HTML content
							let url = data;
							url += (url.includes('?') ? '&' : '?') + 'theme=' + vc.theme + vc.timeRange + '&kiosk&var-instance=';

							$.get(url)
							.done(function(data, textStatus, jqXHR) {
								vc.grafanaUrl = url;
							})
							.fail(function( jqXHR, textStatus, errorThrown ) {
								vc.notifyGrafanaError();
							});
						} else {
							vc.notifyGrafanaError();
						}
					})
					.fail(function( jqXHR, textStatus, errorThrown ) {
						if(textStatus == 'error') {
							vc.notifyGrafanaError();
						}       
					})
				}

				return store.state.clusters
			},

			theme () {
				return store.state.theme
			},

		},

		mounted: function() {

		},
		
		methods: {

			notifyGrafanaError() {
				this.notify({
					title: '',
					detail: 'There was a problem when trying to access Grafana\'s dashboard. Please confirm the cluster is functioning properly and that you have correctly setup the operator\'s credentials to view Grafana.',
					type: 'https://stackgres.io/doc/latest/install/prerequisites/monitoring/#installing-grafana-and-create-basic-dashboards',
					status: 403
				},'error')
				$('#grafana').remove();
			}
		}
	}
</script>

<style scoped>
	header select {
		position: absolute;
		right: 0;
		top: 102px;
		background-position-x: 90%;
		width: 160px;
		text-align: left;
	}

	header select.active {
		background-color: var(--bgColor) !important;
	}

	select li.selected {
		padding: 0;
	}

	select.active li.selected a {
		background: var(--borderColor);
	}

	select.active li:first-child {
		border-bottom: 1px solid var(--blue);
	}

	select.active li.selected {
		border: 0;
	}

	select:not(.active) a:hover {
		background: transparent;
	}

	#timeRange {
		right: 170px;
	}

	#timeRange.active {
	    max-height: 40vh;
		overflow-y: auto;
		width: auto;
	}	
</style>