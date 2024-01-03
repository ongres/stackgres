<template>
	<div id="grafana" v-if="iCanLoad">
        <div class="content grafana">
            <template v-if="hasActivePods">
                <div class="grafanaActions">
                    <select class="plain capitalize" id="dashboardsList" v-model="dashboard" @change="goTo(dashboardUrl)">
						<option disabled value="">
							Choose dashboard
						</option>
						<template v-for="dashboard in dashboardsList">
							<option :value="dashboard" :key="'dashboard-' + dashboard.name">
								{{ dashboard.name.replaceAll('-',' ') }}
							</option>
						</template>
					</select>
					
					<select class="plain" id="timeRange" v-model="timeRange" @change="goTo(dashboardUrl)">
                        <option disabled value=""><strong>Choose time range</strong></option>
                        <option v-for="(time, id) in timeRangeOptions" :value="id">
                            {{ time.label }}
                        </option>
                    </select>

                    <select class="plain" v-model="selectedNode" @change="goTo(dashboardUrl)">
                        <option disabled value=""><strong>Choose node</strong></option>
                        <option v-for="pod in clusterPods" v-if="pod.status == 'Active'" :value="pod.name">
                            {{ pod.name }}
                            <template v-if="(pod.role == 'primary')"><span>(primary) </span></template>
                        </option>
                        <template v-if="clusterPods.filter(p => (p.status != 'Active')).length">
                            <option disabled value="">--</option>
                            <option disabled value="">Inactive nodes:</option>
                            <option v-for="pod in clusterPods" v-if="pod.status != 'Active'" :value="pod.name" disabled>
                                {{ pod.name }}
                            </option>
                        </template>
                    </select>
                </div>

                <iframe
					v-if="grafanaUrl.length"
					:src="grafanaUrl + 
						'var-instance=' + selectedNodeIp +
						'&var-pod=' + selectedNode +
						($route.params.hasOwnProperty('range')
							? timeRangeOptions[timeRange].range
							: ''
						)
					"
					id="grafana"
				></iframe>
            </template>
            <div v-else class="warningText">
                No active pods have been found for this cluster
            </div>
        </div>
	</div>
</template>

<script>
	import router from '@/router'
	import store from '@/store'
	import { mixin } from '../../mixins/mixin'


    export default {
        name: 'ShardedClusterMonitoring',

		mixins: [mixin],

		data: function() {

			return {
				dashboard: {
					name: this.$route.params.hasOwnProperty('dashboard') ? this.$route.params.dashboard : 'current-activity',
					url: ''
				},
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

			cluster() {
				let vc = this;
				let cluster = store.state.sgshardedclusters.find(c => ((vc.$route.params.namespace == c.data.metadata.namespace) && (vc.$route.params.name == c.name)));

				if(typeof cluster != 'undefined') {

					let clusterPods = this.hasProp(cluster, 'stats.coordinator.pods') && this.hasProp(cluster, 'stats.coordinator.pods') 
						? cluster.stats.coordinator.pods.concat(cluster.stats.shards.pods)
						: []

					// Read Grafana URL
					if(!vc.$route.params.hasOwnProperty('pod')) {
						
						let primaryNode = clusterPods.find(p => (p.role == 'primary'));

						if(typeof primaryNode != 'undefined') {
							if(vc.$route.path.endsWith('/'))
								router.replace(vc.$route.path + primaryNode.name + '/' + vc.dashboard.name)
							else 
								router.replace(vc.$route.path + '/' + primaryNode.name + '/' + vc.dashboard.name)
						}
					} else if(typeof clusterPods.find(p => (p.name == vc.$route.params.pod)) != 'undefined') {

							if(vc.$route.params.hasOwnProperty('dashboard') && (typeof vc.dashboardsList.find( d => d.name == vc.$route.params.dashboard) === 'undefined')) {
								vc.notify('The dashboard specified in the URL could not be found, you\'ve been redirected to the default dashboard (Current Activity)', 'message', 'sgshardedclusters');
								vc.dashboard = {
									name: 'current-activity',
									url: ''
								};
								vc.goTo(vc.dashboardUrl);
							}
							
							if(vc.$route.params.hasOwnProperty('range') && !vc.timeRangeOptions.hasOwnProperty(vc.$route.params.range)) {
								vc.notify('The timerange specified in the URL could not be found, you\'ve been redirected to the default timerange (last 1 hour)', 'message', 'sgshardedclusters');
								vc.timeRange = '';
								vc.goTo(vc.dashboardUrl);
							}

					} else {
						store.commit('notFound',true)
					}

                    return cluster;
				} else {
                    return null
                }
			},

			theme() {
				return store.state.theme
			},

			grafanaUrl() {
				if(this.dashboard.url.length) {
					return (
						this.dashboard.url +
						(this.dashboard.url.includes('?') ? '&' : '?') +
						'theme=' + this.theme + '&kiosk&'
					);
				} else {
					return '';
				}
			},

			dashboardUrl() {
				return '/' + this.$route.params.namespace + '/sgshardedcluster/' + this.$route.params.name + '/monitor/' + this.selectedNode + '/' + this.dashboard.name + '/' + this.timeRange;
			},

			dashboardsList() {
				const vc = this;
				
				if(vc.$route.params.hasOwnProperty('dashboard')) {
					vc.dashboard = store.state.dashboardsList.find( d => d.name == vc.$route.params.dashboard);
				} else {
					let defaultDashboard = store.state.dashboardsList.find( d => d.name == 'current-activity');
					vc.dashboard = (typeof defaultDashboard !== 'undefined') ? defaultDashboard : store.state.dashboardsList[0];
				}

				return store.state.dashboardsList
			},

            hasActivePods() {
				return ( 
                    (   
                        this.hasProp(this.cluster, 'stats.coordinator.pods') && 
                        this.cluster.stats.coordinator.pods.filter(p => (p.status == 'Active')).length
                    ) || 
                    (   
                        this.hasProp(this.cluster, 'stats.shards.pods') && 
                        this.cluster.stats.shards.pods.filter(p => (p.status == 'Active')).length
                    )
                )
			},

            clusterPods() {
                if (this.hasProp(this.cluster, 'stats.coordinator.pods') && this.hasProp(this.cluster, 'stats.coordinator.pods')) {
                    return this.cluster.stats.coordinator.pods.concat(this.cluster.stats.shards.pods);
                } else {
                    return [];
                }
            },

			selectedNodeIp() {
				return this.selectedNode.length ? this.clusterPods.find(p => (p.name == this.selectedNode)).ip : '';
			}
		},

	}
</script>

<style scoped>
	.grafana {
		position: relative;
	}

	.grafanaActions {
		position: absolute;
		right: 0;
		top: -63px;
	}

	.grafanaActions select {
		margin-top: 4px;
		margin-left: 10px;
		text-align: left;
		width: auto;
		min-width: 200px;
	}
	
	#timeRange.active {
	    max-height: 40vh;
		overflow-y: auto;
		width: auto;
	}	
</style>