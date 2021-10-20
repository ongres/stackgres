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
						Events
					</li>
				</ul>

				<div class="actions">
					<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
					<div>
						<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGClusters', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
						<router-link v-if="iCan('create','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/edit'">Edit Cluster</router-link>
						<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgclusters', $route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/sgclusters')" :class="$route.params.namespace + '/sgclusters'">Delete Cluster</a>
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
						<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/events'" title="Events" class="events router-link-exact-active">Events</router-link>
					</li>
				</ul>
			</header>
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
	import axios from 'axios'
	import moment from 'moment'

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
				
				axios
				.get('/stackgres/namespaces/' + vc.$route.params.namespace + '/sgclusters/' + vc.$route.params.name + '/events')
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
				return store.state.clusters
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