<template>
	<div id="grafana" v-if="loggedIn && isReady && !notFound">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
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
        name: 'ClusterMonitoring',

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
				let cluster = store.state.sgclusters.find(c => ((vc.$route.params.namespace == c.data.metadata.namespace) && (vc.$route.params.name == c.name)));

				if(typeof cluster != 'undefined') {

					// Read Grafana URL
					if(!vc.$route.params.hasOwnProperty('pod')) {
						
						let primaryNode = cluster.data.pods.find(p => (p.role == 'primary'));

						if(typeof primaryNode != 'undefined') {
							if(vc.$route.path.endsWith('/'))
								router.replace(vc.$route.path + primaryNode.ip)
							else 
								router.replace(vc.$route.path + '/' + primaryNode.ip)
						}
					} else {

						if(typeof cluster.data.pods.find(p => (p.ip == vc.$route.params.pod)) != 'undefined') {

							if(vc.$route.params.hasOwnProperty('range') && !vc.timeRangeOptions.hasOwnProperty(vc.$route.params.range)) {
								store.commit('notFound',true)
								return false;
							}

							$.get("/grafana")
							.done(function( data, textStatus, jqXHR ) {
								
								if(!data.startsWith('<!DOCTYPE html>')) { // Check "/grafana" isn't just returning web console's HTML content
									vc.grafanaUrl = data + (data.includes('?') ? '&' : '?') + 'theme=' + vc.theme + '&kiosk&var-instance=';
								} else {
									vc.notifyGrafanaError();
								}
							})
							.fail(function( jqXHR, textStatus, errorThrown ) {
								if(textStatus == 'error') {
									vc.notifyGrafanaError();
								}       
							})

						} else {
							store.commit('notFound',true)
						}
					}

				}

				return store.state.sgclusters
			},

			theme () {
				return store.state.theme
			},

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