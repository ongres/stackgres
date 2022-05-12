<template>
	<div id="cluster-info" v-if="loggedIn && isReady && !notFound">
		<template v-for="cluster in clusters" v-if="(cluster.name == $route.params.name) && (cluster.data.metadata.namespace == $route.params.namespace)">
            <div class="content">
                <template v-if="!$route.params.hasOwnProperty('uid')">
                    <table id="events" class="resizable" v-columns-resizable>
                        <thead>
							<th class="firstTimestamp hasTooltip" data-type="timestamp">
                                <span title="First Timestamp">
                                    First Timestamp
                                </span>
                            </th>
                            <th class="lastTimestamp hasTooltip" data-type="timestamp">
                                <span title="Last Timestamp">
                                    Last Timestamp
                                </span>
                            </th>
                            <th class="involvedObject hasTooltip" data-type="involvedObject" v-if="showInvolvedObjectsColumn">
                                <span title="Component">
                                    Component
                                </span>
                            </th>
                            <th class="eventMessage hasTooltip">
                                <span title="Message">
                                    Message
                                </span>
                            </th>
                        </thead>
                        <tbody>
                            <template v-if="!events.length">
                                <tr class="no-results">
                                    <td colspan="999">
                                        No recent events have been recorded for this cluster
                                    </td>
                                </tr>
                            </template>
                            <template v-else>
                                <template v-for="(event, index) in events">
                                    <template v-if="(index >= pagination.start) && (index < pagination.end)">
                                        <tr class="base">
											<td class="timestamp hasTooltip">
												<span v-if="event.hasOwnProperty('firstTimestamp')">
													<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
                                                        <span class='date'>
                                                            {{ event.firstTimestamp | formatTimestamp('date') }}
                                                        </span>
                                                        <span class='time'>
                                                            {{ event.firstTimestamp | formatTimestamp('time') }}
                                                        </span>
                                                        <span class='ms'>
                                                            {{ event.firstTimestamp | formatTimestamp('ms') }}
                                                        </span>
                                                        <span class='tzOffset'>{{ showTzOffset() }}</span>
													</router-link>
												</span>
											</td>
											<td class="timestamp hasTooltip">
												<span v-if="event.hasOwnProperty('lastTimestamp')">
													<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
                                                        <span class='date'>
                                                            {{ event.lastTimestamp | formatTimestamp('date') }}
                                                        </span>
                                                        <span class='time'>
                                                            {{ event.lastTimestamp | formatTimestamp('time') }}
                                                        </span>
                                                        <span class='ms'>
                                                            {{ event.lastTimestamp | formatTimestamp('ms') }}
                                                        </span>
                                                        <span class='tzOffset'>{{ showTzOffset() }}</span>
													</router-link>
												</span>
											</td>
                                            <td class="involvedObject hasTooltip" v-if="showInvolvedObjectsColumn">
												<span>
													<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
														{{ event.involvedObject.kind }}/{{ event.involvedObject.name }}
													</router-link>
												</span>
                                            </td>
                                            <td class="eventMessage hasTooltip">
                                                <span>
                                                    <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
                                                        {{ event.message }}
                                                    </router-link>
                                                </span>
                                            </td>
                                        </tr>
                                    </template>
                                </template>
                            </template>
                        </tbody>
                    </table>
                    <v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < events.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="events.length" @page-change="pageChange" align="center" ref="page"></v-page>
                    <div id="nameTooltip">
                        <div class="info"></div>
                    </div>
                </template>
                <template v-else>
                    <div class="relative">
                        <h2>Event Details</h2>
                        <div class="titleLinks">
                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/events'" title="Close Details">Close Details</router-link>
                        </div>
                    </div>
                    <div class="configurationDetails" v-for="event in events" v-if="event.metadata.uid == $route.params.uid">
                        <table class="crdDetails">
                            <tbody>
                                <tr>
                                    <td class="label">Name</td>
                                    <td>{{ event.metadata.name }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('type') && event.type">
                                    <td class="label">Type</td>
                                    <td>{{ event.type }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('firstTimestamp') && event.firstTimestamp">
                                    <td class="label">First Timestamp</td>
                                    <td class="timestamp">
										<span class='date'>
											{{ event.firstTimestamp | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ event.firstTimestamp | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ event.firstTimestamp | formatTimestamp('ms') }}
										</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('lastTimestamp') && event.lastTimestamp">
                                    <td class="label">Last Timestamp</td>
                                    <td class="timestamp">
										<span class='date'>
											{{ event.lastTimestamp | formatTimestamp('date') }}
										</span>
										<span class='time'>
											{{ event.lastTimestamp | formatTimestamp('time') }}
										</span>
										<span class='ms'>
											{{ event.lastTimestamp | formatTimestamp('ms') }}
										</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('count') && event.count">
                                    <td class="label">Count</td>
                                    <td>{{ event.count }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('reason') && event.reason">
                                    <td class="label">Reason</td>
                                    <td>{{ event.reason }}</td>
                                </tr><tr v-if="event.hasOwnProperty('action') && event.action">
                                    <td class="label">Action</td>
                                    <td>{{ event.action }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('involvedObject') && event.involvedObject">
                                    <td class="label">Involved Object</td>
                                    <td>{{ event.involvedObject.kind }}/{{ event.involvedObject.name }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('reportingComponent') && event.reportingComponent">
                                    <td class="label">Reporting Component</td>
                                    <td class="vPad">{{ event.reportingComponent }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('reportingInstance') && event.reportingInstance">
                                    <td class="label">Reporting Instance</td>
                                    <td class="vPad">{{ event.reportingInstance }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('related') && event.related">
                                    <td class="label">Related</td>
                                    <td>{{ event.related }}</td>
                                </tr>
								<tr v-if="event.hasOwnProperty('message') && event.message">
                                    <td class="label">Message</td>
                                    <td>{{ event.message }}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </template>
            </div>
        </template>
	</div>
</template>

<script>
	import store from '../store'
	import { mixin } from './mixins/mixin'
	import moment from 'moment'
	import sgApi from '../api/sgApi'

    export default {
        name: 'ClusterEvents',

		mixins: [mixin],

		data: function() {
			return {
				events: [],
				eventsPooling: null,
				showInvolvedObjectsColumn: false
			}
		},

        mounted: function() {
			const vc = this;

			vc.getClusterEvents();
			vc.eventsPooling = setInterval( function() {
				vc.getClusterEvents()
			}, 5000);
		},

		methods: {

            getClusterEvents() {
				const vc = this;
				
				sgApi
				.getResourceDetails('sgclusters', vc.$route.params.namespace, vc.$route.params.name, 'events')
				.then( function(response) {
					vc.events = [...response.data]
					
					vc.events.sort((a,b) => {
						
						if(moment(a.firstTimestamp).isValid && moment(b.firstTimestamp).isValid) {

							if(moment(a.firstTimestamp).isBefore(moment(b.firstTimestamp)))
								return 1;
						
							if(moment(a.firstTimestamp).isAfter(moment(b.firstTimestamp)))
								return -1;  

						}
					});

					vc.showInvolvedObjectsColumn = (vc.events.filter(e => (e.involvedObject.kind != 'SGCluster') ).length > 0)
				}).catch(function(err) {
					console.log(err);
					vc.checkAuthError(err);
				});
			},
		},

		computed: {

			tooltips () {
				return store.state.tooltips
			},

			clusters () {
				return store.state.sgclusters
			},

		},

		beforeDestroy () {
			clearInterval(this.eventsPooling);
		} 
	}
</script>

<style scoped>
	table.resizable th[data-type="involvedObject"] {
		min-width: 150px;
		max-width: 250px;
	}
</style>