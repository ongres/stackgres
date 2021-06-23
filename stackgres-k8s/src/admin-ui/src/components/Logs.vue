<template>
	<div id="sg-logs" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
					<router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">SGClusters</router-link>
				</li>
				<li>
					<router-link :to="'/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status">{{ $route.params.name }}</router-link>
				</li>
				<li>
					Logs
				</li>
			</ul>

			<div class="actions">
			<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
				<div>
					<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
					<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/crd/edit/cluster/'+$route.params.namespace+'/'+$route.params.name">Edit Cluster</router-link>
					<a v-if="iCan('delete','sgclusters',$route.params.namespace)" v-on:click="deleteCRD('sgcluster', $route.params.namespace, $route.params.name, '/overview/'+$route.params.namespace)" :class="'/overview/'+$route.params.namespace">Delete Cluster</a>
				</div>
			</div>

			<ul class="tabs">
				<li>
					<router-link :to="'/cluster/status/'+$route.params.namespace+'/'+$route.params.name" title="Status" class="status">Status</router-link>
				</li>
				<li>
					<router-link :to="'/cluster/configuration/'+$route.params.namespace+'/'+$route.params.name" title="Configuration" class="info">Configuration</router-link>
				</li>
				<li v-if="iCan('list','sgbackups',$route.params.namespace)">
					<router-link :to="'/cluster/backups/'+$route.params.namespace+'/'+$route.params.name" title="Backups" class="backups">Backups</router-link>
				</li>
				<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace)">
					<router-link :to="'/cluster/logs/'+$route.params.namespace+'/'+$route.params.name" title="Distributed Logs" class="logs">Logs</router-link>
				</li>
				<li v-if="grafanaEmbedded">
					<router-link id="grafana-btn" :to="'/cluster/monitor/'+$route.params.namespace+'/'+$route.params.name" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
				</li>
			</ul>
		</header>

		<div class="content noScroll">
			<div id="logs">
				<div class="toolbar">
					<div class="searchBar" :class="(text.length ? 'filtered' : '')" >
						<input id="keyword" v-model="text" class="search" placeholder="Search text..." @keyup="toggleClear('keyword')" autocomplete="off">
						<a @click="getLogs()" class="btn">APPLY</a>
						<a @click="clearFilters('keyword')" class="btn clear border keyword" style="display:none">CLEAR</a>
					</div>

					<div class="filter">
						<span class="toggle date">DATE RANGE <input v-model="datePicker" id="datePicker" autocomplete="off"></span>
					</div>

					<div class="filter filters">
						<span class="toggle">FILTER</span>

						<ul class="options">
							<li>
								<span>Type</span>
								<label for="logType">
									<span>Postgres</span>
									<input @change="xCheckbox('logType','pg')" v-model="logType" data-filter="logType" type="checkbox" class="xCheckbox" id="logTypepg" name="logTypepg" value="pg"/>
								</label>
								<label for="logType">
									<span>Patroni</span>
									<input @change="xCheckbox('logType','pa')" v-model="logType" data-filter="logType" type="checkbox" class="xCheckbox" id="logTypepa" name="logTypepa" value="pa"/>
								</label>
							</li>

							<li>
								<span>Role</span>
								<select v-model="role" @change="toggleClear('filters')">
									<option value=''>All Roles</option>
									<option>Primary</option>
									<option>Replica</option>
									<option>Promoted</option>
									<option>Demoted</option>
									<option>Uninitialized</option>
									<option>Standby</option>
								</select>
							</li>

							<li>
								<span>Error Level</span>
								<select v-model="errorLevel" @change="toggleClear('filters')">
									<option value=''>All levels</option>
									<option>PANIC</option>
									<option>CRITICAL</option>
									<option>FATAL</option>
									<option>LOG</option>
									<option>ERROR</option>
									<option>WARNING</option>
									<option>NOTICE</option>
									<option>INFO</option>
									<option>DEBUG</option>
									<option>NOT SET</option>
								</select>
							</li>
							<li class="textFilter">
								<span>Pod Name</span>
								<input v-model="podName" class="search" @keyup="toggleClear('filters')">
								<span class="btn clear border" @click="clearFilters('podName')" v-if="podName.length">×</span>
							</li>
							<li class="textFilter">
								<span>User Name</span>
								<input v-model="userName" class="search" @keyup="toggleClear('filters')">
								<span class="btn clear border" @click="clearFilters('userName')" v-if="userName.length">×</span>
							</li>
							<li class="textFilter">
								<span>Database Name</span>
								<input v-model="databaseName" class="search" @keyup="toggleClear('filters')">
								<span class="btn clear border" @click="clearFilters('databaseName')" v-if="databaseName.length">×</span>
							</li>
							<li>
								<hr>
								<a class="btn" @click="getLogs()">APPLY</a> <a class="btn clear border" @click="clearFilters('filters')" style="display:none">CLEAR</a>
							</li>
						</ul>
					</div>

					<div class="filter columns">
						<span class="toggle">VISIBLE FIELDS</span>

						<ul class="options">
							<li>
								<label for="viewLogType">
									<span>Log Type</span>
									<input @change="toggleColumn('logType')" type="checkbox" id="viewLogType" checked/>
								</label>
							</li>
							<li>
								<label for="viewErrorLevel">
									<span>Error Level</span>
									<input @change="toggleColumn('errorLevel')" type="checkbox" id="viewErrorLevel" checked/>
								</label>
							</li>
							<li>
								<label for="viewPodName">
									<span>Pod Name</span>
									<input @change="toggleColumn('podName')" type="checkbox" id="viewPodName" checked/>
								</label>
							</li>
							<li>
								<label for="viewRole">
									<span>Role</span>
									<input @change="toggleColumn('role')" type="checkbox" id="viewRole" checked/>
								</label>
							</li>
							<li>
								<label for="viewLogMessage">
									<span>Message</span>
									<input @change="toggleColumn('message')" type="checkbox" id="viewLogMessage" checked/>
								</label>
							</li>
							<li>
								<label for="viewUserName">
									<span>User</span>
									<input @change="toggleColumn('userName')" type="checkbox" id="viewUserName" checked/>
								</label>
							</li>
							<li>
								<label for="viewDatabaseName">
									<span>Database</span>
									<input @change="toggleColumn('databaseName')" type="checkbox" id="viewDatabaseName" checked/>
								</label>
							</li>
							<li>
								<label for="viewProcessId">
									<span>Process ID</span>
									<input @change="toggleColumn('processId')" type="checkbox" id="viewProcessId" checked/>
								</label>
							</li>
							<li>
								<label for="viewConnectionFrom">
									<span>Connection From</span>
									<input @change="toggleColumn('connectionFrom')" type="checkbox" id="viewConnectionFrom" checked/>
								</label>
							</li>
							<li>
								<label for="viewApplicationName">
									<span>Application</span>
									<input @change="toggleColumn('applicationName')" type="checkbox" id="viewApplicationName" checked/>
								</label>
							</li>
						</ul>
					</div>

					<!--<div class=calendar>
						<ul>
							<li><span class="shortcut" @click="setTime('1m')">1 m</span></li>
							<li><span class="shortcut" @click="setTime('30m')">30 m</span></li>
							<li><span class="shortcut" @click="setTime('3h')">3 h</span></li>
							<li><span class="shortcut" @click="setTime('1d')">1 d</span></li>
							<li><span><svg xmlns="http://www.w3.org/2000/svg" width="15.002" height="16.503" viewBox="0 0 15.002 16.503"><g transform="translate(-3.75 -2.25)"><path d="M6,6H16.5A1.5,1.5,0,0,1,18,7.5V18a1.5,1.5,0,0,1-1.5,1.5H6A1.5,1.5,0,0,1,4.5,18V7.5A1.5,1.5,0,0,1,6,6Z" transform="translate(0 -1.499)" fill="none" stroke="#7a7b85" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"/><path d="M24,3V6" transform="translate(-9.749)" fill="none" stroke="#7a7b85" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"/><path d="M12,3V6" transform="translate(-3.75)" fill="none" stroke="#7a7b85" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"/><path d="M4.5,15H18" transform="translate(0 -5.999)" fill="none" stroke="#7a7b85" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"/></g></svg> <input v-model="datePicker" id="datePicker" autocomplete="off"></span></li>
						</ul>
					</div>-->
				</div>

				<div class="logsContainer monoFont" v-on:scroll.passive="handleScroll">
					<ul class="legend">
						<li class="field errorLevel" v-if="showColumns.errorLevel">
							<span title="Error Level">Error Level</span>
							<span class="helpTooltip" data-tooltip='Error Level of the log entry. Available levels are:<br/><ul class="monoFont errorLevelLegend"><li class="not-set">Not Set</li><li class="debug">Debug</li><li class="info">Info</li><li class="notice">Notice</li><li class="warning">Warning</li><li class="error">Error</li><li class="log">Log</li><li class="fatal">Fatal</li><li class="critical">Critical</li><li class="panic">Panic</li></ul>'></span>
						</li>
						<li class="field logTime">
							<span title="Log Time">Log Time</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.logTime')"></span>
						</li>
						<li class="field logType" v-if="showColumns.logType">
							<span title="Type">Type</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.logType')"></span>
						</li>
						<li class="field podName" v-if="showColumns.podName">
							<span title="Pod Name">Pod Name</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.podName')"></span>
						</li>
						<li class="field role" v-if="showColumns.role">
							<span title="Role">Role</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.role')"></span>
						</li>
						<li class="field userName" v-if="showColumns.userName">
							<span title="User">User</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.userName')"></span>
						</li>
						<li class="field databaseName" v-if="showColumns.databaseName">
							<span title="Database">Database</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.databaseName')"></span>
						</li>
						<li class="field processId" v-if="showColumns.processId">
							<span title="Process ID">Process ID</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.processId')"></span>
						</li>
						<li class="field connectionFrom" v-if="showColumns.connectionFrom">
							<span title="Connection From">Connection From</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.connectionFrom')"></span>
						</li>
						<li class="field applicationName" v-if="showColumns.applicationName">
							<span title="Application">Application</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.applicationName')"></span>
						</li>
						<li class="field message" v-if="showColumns.message">
							<span title="Message">Message</span>
							<span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.message')"></span>
						</li>
					</ul>
					<div class="records">
						<template v-for="(log, logIndex) in logs">
							<div class="log" :class="( (logIndex == currentLog) ? 'open' : '' )">
								<span class="errorLevel" :class="log.errorLevel.toLowerCase().replace(' ','-')" :title="'Error Level: ' + log.errorLevel" v-if="showColumns.errorLevel"></span>
								<div v-if="logIndex != currentLog" class="base" @click="currentLog = logIndex">
									<span class="field timestamp logTime">
										<span class='date'>
											{{ log.logTime | formatTimestamp('date') }}
										</span>
										<span class='time'>{{ log.logTime | formatTimestamp('time') }}</span><span class='ms'>{{ log.logTime | formatTimestamp('ms') }}</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</span>
									<template v-for="(value, prop) in showColumns" v-if="( value && (!['logTime','errorLevel'].includes(prop)) )">
										<span class="field" :class="prop" v-if="log.hasOwnProperty(prop)">{{ log[prop] }}</span>
									</template>
								</div>
								<div v-else class="details">
									<span class="field timestamp logTime">
										<span class='date'>
											{{ log.logTime | formatTimestamp('date') }}
										</span>
										<span class='time'>{{ log.logTime | formatTimestamp('time') }}</span><span class='ms'>{{ log.logTime | formatTimestamp('ms') }}</span>
										<span class='tzOffset'>{{ showTzOffset() }}</span>
									</span>
									<span class="closeDetails" @click="currentLog = -1">Close Details</span>
									<ul class="fields">
										<li class="logMessage">
											<span class="param">Message <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.message')"></span></span>
											<span class="value">{{ log.message }}</span>
										</li>
										<li>
											<span class="param">Type <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.logType')"></span></span>
											<span class="field value label logType upper">{{ log.logType }}</span>
										</li>
										<li v-if="log.hasOwnProperty('podName')">
											<span class="param">Pod Name <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.podName')"></span></span>
											<span class="field value podName">{{ log.podName }}</span>
										</li>
										<li v-if="log.hasOwnProperty('role')">
											<span class="param">Role <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.role')"></span></span>
											<span class="field value label role" :class="log.role">
												<span>{{ log.role }}</span>
											</span>
										</li>
										<li v-if="log.hasOwnProperty('userName')">
											<span class="param">User <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.userName')"></span></span>
											<span class="field value userName">{{ log.userName }}</span>
										</li>
										<li v-if="log.hasOwnProperty('databaseName')">
											<span class="param">Database <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.databaseName')"></span></span>
											<span class="field value databaseName">{{ log.databaseName }}</span>
										</li>
										<li v-if="log.hasOwnProperty('processId')">
											<span class="param">Process ID <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.processId')"></span></span>
											<span class="field value processId">{{ log.processId }}</span>
										</li>
										<li v-if="log.hasOwnProperty('connectionFrom')">
											<span class="param">Connection From <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.connectionFrom')"></span></span>
											<span class="field value connectionFrom">{{ log.connectionFrom }}</span>
										</li>
										<li v-if="log.hasOwnProperty('errorLevel')">
											<span class="param">Error Level <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.errorLevel')"></span></span>
											<span class="field value label errorLevel" :class="log.errorLevel.toLowerCase().replace(' ','-')"><span>{{ log.errorLevel }}</span></span>
										</li>
										<li v-if="log.hasOwnProperty('sessionId')">
											<span class="param">Session ID <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.sessionId')"></span></span>
											<span class="field value">{{ log.sessionId }}</span>
										</li>
										<li v-if="log.hasOwnProperty('sessionLineNum')">
											<span class="param">Session Line Number <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.sessionLineNum')"></span></span>
											<span class="field value">{{ log.sessionLineNum }}</span>
										</li>
										<li v-if="log.hasOwnProperty('commandTag')">
											<span class="param">Command Tag <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.commandTag')"></span></span>
											<span class="field value">{{ log.commandTag }}</span>
										</li>
										<li v-if="log.hasOwnProperty('sessionStartTime')">
											<span class="param">Session Start Time <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.sessionStartTime')"></span></span>
											<span class="field value">{{ log.sessionStartTime }}</span>
										</li>
										<li v-if="log.hasOwnProperty('virtualTransactionId')">
											<span class="param">Virtual Transaction ID <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.virtualTransactionId')"></span></span>
											<span class="field value">{{ log.virtualTransactionId }}</span>
										</li>
										<li v-if="log.hasOwnProperty('transactionId')">
											<span class="param">Transaction ID <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.transactionId')"></span></span>
											<span class="field value">{{ log.transactionId }}</span>
										</li>
										<li v-if="log.hasOwnProperty('sqlStateCode')">
											<span class="param">SQL State Code <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.sqlStateCode')"></span></span>
											<span class="field value">{{ log.sqlStateCode }}</span>
										</li>
										<li v-if="log.hasOwnProperty('detail')">
											<span class="param">Detail <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.detail')"></span></span>
											<span class="field value">{{ log.detail }}</span>
										</li>
										<li v-if="log.hasOwnProperty('hint')">
											<span class="param">Hint <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.hint')"></span></span>
											<span class="field value">{{ log.hint }}</span>
										</li>
										<li v-if="log.hasOwnProperty('internalQuery')">
											<span class="param">Internal Query <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.internalQuery')"></span></span>
											<span class="field value">{{ log.internalQuery }}</span>
										</li>
										<li v-if="log.hasOwnProperty('internalQueryPos')">
											<span class="param">Internal Query Pos <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.internalQueryPos')"></span></span>
											<span class="field value">{{ log.internalQueryPos }}</span>
										</li>
										<li v-if="log.hasOwnProperty('context')">
											<span class="param">Context <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.context')"></span></span>
											<span class="field value">{{ log.context }}</span>
										</li>
										<li v-if="log.hasOwnProperty('query')">
											<span class="param">Query <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.query')"></span></span>
											<span class="field value">{{ log.query }}</span>
										</li>
										<li v-if="log.hasOwnProperty('queryPos')">
											<span class="param">Query Pos <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.queryPos')"></span></span>
											<span class="field value">{{ log.queryPos }}</span>
										</li>
										<li v-if="log.hasOwnProperty('location')">
											<span class="param">Location <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.location')"></span></span>
											<span class="field value">{{ log.location }}</span>
										</li>
										<li v-if="log.hasOwnProperty('applicationName')">
											<span class="param">Application Name <span class="helpTooltip" :data-tooltip="getTooltip('sgclusterlogentry.applicationName')"></span></span>
											<span class="field value">{{ log.applicationName }}</span>
										</li>
									</ul>
								</div>
							</div>
						</template>
					</div>
				</div>
			</div>
		</div>
		<div id="logTooltip">
			<div class="info"></div>
		</div>
	</div>
</template>

<script>
	import store from '../store'
	import axios from 'axios'
	import { mixin } from './mixins/mixin'

    export default {
        name: 'Logs',

		mixins: [mixin],

		data: function() {

			return {
				currentSortDir: 'desc',
				records: 50,
				fetching: false,
				lastCall: '',
				currentLog: -1,
				text: '',
				logType: [],
				errorLevel: '',
				podName: '',
				role: '',
				userName: '',
				databaseName: '',
				datePicker: '',
				dateStart: '',
				dateEnd: '',
				showColumns: {
					errorLevel: true,
					logTime: true,
					logType: true,
					podName: true,
					role: true,
					userName: true,
					databaseName: true,
					processId: true,
					connectionFrom: true,
					applicationName: true,
					message: true
				},
			}
		},
		computed: {

			clusters () {
				return store.state.clusters
			},

			logs() {
				return [...store.state.logs]
			},
			
			grafanaEmbedded() {
				var grafana = false;
				const vm = this;
				
				store.state.clusters.forEach(function( c ){
					if( (c.data.metadata.name === vm.$route.params.name) && (c.data.metadata.namespace === vm.$route.params.namespace) && c.data.grafanaEmbedded ) {
						grafana = true;
						return false;
					}
				});
				
				return grafana            
			},

			filteredColumns() {
				var filtered = false;
				const vm = this;

				Object.entries(vm.showColumns).forEach(([key, value]) => {
					if(!value) {
						filtered = true;
						return false;
					}
				});
					
				return filtered
			},

			cluster() {
				return store.state.currentCluster
			},

			tooltips() {
				return store.state.tooltips
			}

		},
		mounted: function() {

			// Load datepicker
			require('daterangepicker');
						
			const vc = this;

			$(document).ready(function(){

				$(document).on('focus', '#datePicker', function() {

                    if(!$(this).val()) {
						
						$('#datePicker').daterangepicker({
							"parentEl": "#log",
							"autoApply": true,
							"timePicker": true,
							"timePicker24Hour": true,
							"timePickerSeconds": true,
							"opens": "left",
							locale: {
								cancelLabel: "Clear"
							}
						}, function(start, end, label) {

							if(vc.currentSortDir === 'asc') {
								vc.dateStart = start.format('YYYY-MM-DDTHH:mm:ss');
								vc.dateEnd = end.format('YYYY-MM-DDTHH:mm:ss');
							} else {
								vc.dateEnd = start.format('YYYY-MM-DDTHH:mm:ss');
								vc.dateStart = end.format('YYYY-MM-DDTHH:mm:ss');
							}

							vc.datePicker = vc.dateStart+' / '+vc.dateEnd;
							vc.getLogs(false, true);
						});

						$('#datePicker').on('show.daterangepicker', function(ev, picker) {
							//console.log('show.daterangepicker');
							$('#datePicker').parent().addClass('open');
						});

						$('#datePicker').on('hide.daterangepicker', function(ev, picker) {
							//console.log('hide.daterangepicker');
							$('#datePicker').parent().removeClass('open');

							if($('#datePicker').val().length)
								$('#datePicker').parent().parent().addClass('filtered')
							else
								$('#datePicker').parent().parent().removeClass('filtered')
						});

						$('#datePicker').on('cancel.daterangepicker', function(ev, picker) {
							//console.log('cancel.daterangepicker');
							vc.datePicker = '';
							vc.dateStart = '';
							vc.dateEnd = '';

							$('#datePicker').parent().parent().removeClass('filtered')
							
							vc.getLogs();
							$('#datePicker').parent().removeClass('open');
						});

						$('#datePicker').on('apply.daterangepicker', function(ev, picker) {
							//console.log('apply.daterangepicker');
							$('#datePicker').parent().removeClass('open');

							if($('#datePicker').val().length)
								$('#datePicker').parent().parent().addClass('filtered')
							else
								$('#datePicker').parent().parent().removeClass('filtered')
								
						});

					}
				})
			
				vc.records = parseInt($('.logsContainer').height() / 15);
				vc.getLogs(this.records);

				$(document).on('mousemove', function (e) {

					if( (window.innerWidth - e.clientX) > 420 ) {
						$('#logTooltip').css({
							"top": e.clientY+20, 
							"right": "auto",
							"left": e.clientX+20
						})
					} else {
						$('#logTooltip').css({
							"top": e.clientY+20, 
							"left": "auto",
							"right": window.innerWidth - e.clientX + 20
						})
					}
				})

				$(document).on('mouseenter', 'td.hasTooltip', function(){
					let c = $(this).children('span');
					if(c.width() > $(this).width()){
						$('#logTooltip .info').text(c.text());
						$('#logTooltip').addClass('show');
					}
						
				});

				$(document).on('mouseleave', 'td.hasTooltip', function(){ 
					$('#logTooltip .info').text('');
					$('#logTooltip').removeClass('show');
				});

				$(document).on('click', '.closeLog', function(){
					$(this).parents('tr').prev().toggle();
					$(this).parents('tr').toggleClass('open');
				});

				$(document).on('keyup', 'input.search', function(e){
					if (e.keyCode === 13)
						vc.getLogs();
				});
				
				$(document).on('click', '#datePicker', function(){
					$(this).parent().toggleClass('open');
				});

				$(document).on('change', '.filter select', function () {
					if($(this).val().length)
						$(this).addClass('active')
					else
						$(this).removeClass('active')
				});

				$(window).on('resize', function() {
					if(($('table.logs').height() - 40) > $('table.logs > tbody').height()) {
						vc.records = parseInt((window.innerHeight - 350) / 30);
						vc.getLogs(vc.records);
					}			
				})

			});

		},
		methods: {

			toggleColumn( column ) {
				this.showColumns[column] = !this.showColumns[column]
			},

			xCheckbox(param, value) {

				const vc = this;

				let el = $('#'+param+value);

				if(el.is(':checked')) {
					el.parents('li').find('.active').removeClass('active').prop('checked', false);
					//el.addClass('active');
					el.parent().addClass('active');

					if(vc[el.data('filter')].length)
						vc[el.data('filter')] = [el.val()];
				} else {
					el.parent().removeClass('active');
					vc[el.data('filter')] = [];
				}

				this.toggleClear('filters')

			},

			toggleClear( filter ){

				switch(filter) {
					case 'keyword':
						if($('#keyword').val().length)
							$('.searchBar .clear').fadeIn()
						else
							$('.searchBar .clear').fadeOut()
						
							break;
					case 'filters':
						if($('.filters .options .active').length || $('.filters .options .search').val().length || this.errorLevel.length || this.role.length )
							$('.filters .clear').fadeIn()
						else
							$('.filters .clear').fadeOut()
				}

			},

			clearFilters ( section ) {

				if(section == 'filters') {
					this.logType = [];
					this.errorLevel = '';
					this.podName = '';
					this.role = '';
					this.userName = '';
					this.databaseName = '';
					$('.filter.open .active').removeClass('active');

					$('.filters .clear').fadeOut()

				} else if (section == 'keyword') {
					this.text = '';
					$('#keyword').removeClass('active')

					$('.searchBar .clear').fadeOut()
				} else {
					this[section] = '';
				}
				
				this.getLogs();
			},

			getLogs(append = false, byDate = false) {

				const vc = this;

				$('.logsContainer').addClass('loading');

				let params = '';

				params += '?records='+this.records;
				params += '&sort='+this.currentSortDir;
				
				if(this.dateStart.length && byDate)
					params += '&from='+this.dateStart;
				
				if(this.dateEnd.length && byDate)
					params += '&to='+this.dateEnd;

				if(this.text.length)
					params += '&text='+this.text;

				if(this.logType.length)
					params += '&logType='+this.logType[0];

				if(this.errorLevel.length)
					params += '&errorLevel='+this.errorLevel;

				if(this.podName.length)
					params += '&podName='+this.podName;

				if(this.role.length) {
					params += '&role='+this.role;
				}
				
				if( (store.state.loginToken.search('Authentication Error') == -1) ) {

					let thisCall = '/stackgres/sgcluster/logs/'+this.$route.params.namespace+'/'+this.$route.params.name+params;
					let scrollToBottom = ( ($('.logsContainer')[0].scrollHeight - $('.logsContainer')[0].offsetHeight) == $('.logsContainer').scrollTop() )
					vc.fetching = true;

					axios
					.get(thisCall)
					.then( function(response){

						if(response.data.length) {

							if(append) {
								store.commit('appendLogs', response.data)
							} else {
								store.commit('setLogs', response.data.reverse())
								vc.currentSortDir = 'asc';
							}

							if(scrollToBottom) {
								let scrollAwait = setTimeout(function() {
									if(( ($('.logsContainer')[0].scrollHeight - $('.logsContainer')[0].offsetHeight) != $('.logsContainer').scrollTop() )) {
										$('.logsContainer').scrollTop($('.logsContainer')[0].scrollHeight)
										clearTimeout(scrollAwait)
									}
								},100)
							} 
						}

						$('.logsContainer').removeClass('loading');
						vc.fetching = false;
						
					}).catch(function(err) {
						vc.notify(
							{
							title: 'Error',
							detail: 'There was an error while trying to fetch the information from the API, please refresh the window and try again.'
							},
							'error'
						);

						store.commit('setLogs', []);
						console.log(err);
						vc.checkAuthError(err);

						$('.logsContainer').removeClass('loading');
						vc.fetching = false;
					});

					setTimeout(() => {
						let ltime = store.state.logs[store.state.logs.length-1].logTime;
						let lindex = store.state.logs[store.state.logs.length-1].logTimeIndex;
						vc.dateStart = ltime+','+lindex;
						vc.getLogs(true, true);
					}, 3000);
					
				} else {
					vc.notify(
						{
						title: store.state.loginToken,
						detail: 'There was an authentication error while trying to fetch the information from the API, please refresh the window and try again.'
						},
						'error'
					);
				}

				

			},

			sort() {

				var auxDate = this.dateStart;
				this.dateStart = this.dateEnd;
				this.dateEnd = auxDate;
				
				if(this.currentSortDir == 'desc')
					this.currentSortDir = 'asc'
				else
					this.currentSortDir = 'desc'

				this.getLogs()
			},

			setTime(time) {

				const vc = this;

				switch(time) {

					case '1d':
						if(vc.currentSortDir == 'asc') {
							vc.dateStart = store.state.logs[0].logTime+','+store.state.logs[0].logTimeIndex;

							date.setHours(23,59,59,59);
							vc.dateEnd = date.format('YYYY-MM-DDTHH:mm:ss');
						} else {
							date.setHours(23,59,59,59);
							vc.dateStart = date.format('YYYY-MM-DDTHH:mm:ss');

							date.setHours(0,0,0,0);
							vc.dateEnd = date.format('YYYY-MM-DDTHH:mm:ss');
						}
						
						$('#datePicker').data('daterangepicker').setStartDate(vc.dateStart);
						$('#datePicker').data('daterangepicker').setEndDate(vc.dateEnd);
						break;

				}

			},
			
			handleScroll() {
				/* let vc = this;

				if( ($('.logsContainer').scrollTop() + $('.logsContainer').height() + 200 >= $('.logsContainer')[0].scrollHeight) && store.state.logs.length && !vc.fetching && ($('.logsContainer').get(0).scrollHeight > $('.logsContainer').get(0).clientHeight)) {
					let ltime = store.state.logs[store.state.logs.length-1].logTime;
					let lindex = store.state.logs[store.state.logs.length-1].logTimeIndex;
					vc.dateStart = ltime+','+lindex;
					vc.getLogs(true, true);
				} */
			}

		},
		beforeDestroy: function() {
			store.commit('setLogs', []);
		}
	}
</script>

<style>

	:root {
		--log: #AFAFB0;
		--not-set: #7A7B85;
		--debug: #0A67FC;
		--info: #4E9A06;
		--notice: #32AFFF;
		--warning: #FCDE38;
		--error: #FF6200;
		--fatal: #CC0000;
		--critical: #9D2BF0;
		--panic: #E85FC9;
		--logType: #6762E8;
		--podName: #556B2F;
		--role: #A78904;
		--userName: #2EB9B9;
		--databaseName: #B474AD;
		--processId: #FCC061;
		--connectionFrom: #67A52B;
		--applicationName: #A24D4D;
		--message: #171717;
	}

	.darkmode {
		--message: #E8E8E8;
	}	

	ul.legend {
		position: sticky;
		top: 0;
		background: var(--bgColor);
		box-shadow: 0 10px 40px rgb(255 255 255 / 90%);
		z-index: 1;
		font-size: 14px;
	}

	.darkmode ul.legend {
		box-shadow: 0 10px 60px rgb(0 0 0 / 90%);
	}

	ul.legend li {
		display: inline-block;
		margin: 20px 10px;
	}

	ul.legend span.helpTooltip {
		margin-left: 5px;
	}

	ul.errorLevelLegend li {
		width: 50%;
		float: left;
		text-transform: uppercase;
	}
	
	.logsContainer {
		height: calc(100vh - 330px);
		max-height: calc(100vh - 330px);
		overflow: auto;
	}

	.records {
		padding-top: 15px;
		padding-left: 10px;
	}

	.records > .log {
		font-size: 14px;
		padding: 4px 0;
		display: inline-block;
		width: 100%;
		position: relative;
		margin: 2px 0;
	}

	.records > .log:before {
		content: "▸";
		position: absolute;
		left: -11px;
		font-size: 11px;
		top: 6px;
	}

	.records > .log.open:before {
		content: "▾"
	}

	.log .base {
		max-height: 225px;
    	overflow: hidden;
		display: flex;
		cursor: pointer;
	}

	.log .base span.field {
		margin-left: 10px;
		flex: none;
	}

	.log .field.logType {
		text-transform: uppercase;
	}

	.log .base span.field:nth-last-child(2):not(.timestamp) {
		margin-right: 10px;
	}

	.log span.field.message {
		display: contents;
	}

	.log > .errorLevel {
		width: 4px;
		display: block;
		height: 100%;
		position: absolute;
		border-radius: 5px;
		top: 2px;
	}

	.errorLevelLegend .not-set, .details .not-set {
		color: var(--not-set);
	}

	.errorLevelLegend .debug, .details .debug {
		color: var(--debug);
	}

	.errorLevelLegend .info, .details .info {
		color: var(--info);
	}

	.errorLevelLegend .notice, .details .notice {
		color: var(--notice);
	}

	.errorLevelLegend .warning, .details .warning {
		color: var(--warning);
	}

	.errorLevelLegend .error, .details .error {
		color: var(--error);
	}

	.errorLevelLegend .log, .details .log {
		color: var(--log);
	}

	.errorLevelLegend .fatal, .details .fatal {
		color: var(--fatal);
	}

	.errorLevelLegend .critical, .details .critical {
		color: var(--critical);
	}

	.errorLevelLegend .panic, .details .panic {
		color: var(--panic);
	}

	.log > .errorLevel.not-set {
		background: var(--not-set);
	}

	.log > .errorLevel.debug {
		background: var(--debug);
	}

	.log > .errorLevel.info {
		background: var(--info);
	}

	.log > .errorLevel.notice {
		background: var(--notice);
	}

	.log > .errorLevel.warning {
		background: var(--warning);
	}

	.log > .errorLevel.error {
		background: var(--error);
	}

	.log > .errorLevel.log {
		background: var(--log);
	}

	.log > .errorLevel.fatal {
		background: var(--fatal);
	}

	.log > .errorLevel.critical {
		background: var(--critical);
	}

	.log > .errorLevel.panic {
		background: var(--panic);
	}

	.field.logType {
		color: var(--logType);
	}

	.field.podName {
		color: var(--podName);
	}

	.field.role {
		color: var(--role);
	}

	.field.userName {
		color: var(--userName);
	}

	.field.databaseName {
		color: var(--databaseName);
	}

	.field.processId {
		color: var(--processId);
	}

	.field.connectionFrom {
		color: var(--connectionFrom);
	}

	.field.applicationName {
		color: var(--applicationName);
	}

	.field.message {
		color: var(--message);
	}

	ul.fields li {
		display: inline-block;
		width: 30%;
		padding: 7px 10px;
		border-bottom: 1px solid var(--borderColor);
		margin: 0 1.5%;
	}
	
	ul.fields li.logMessage {
		width: 96%;
		border-top: 1px solid var(--borderColor);
	}

	ul.fields li:not(.logMessage) > span {
		width: 50%;
		display: inline-block;
	}

	.log .details {
		padding: 0 10px;
	}

	.log .details .fields {
		margin: 15px 0;
	}

	.logMessage span.param {
		display: block;
		margin-bottom: 5px;
	}

	.logMessage span.value {
		max-height: 35vh;
		overflow: auto;
		display: inline-block;
		width: 100%;
	}

	/* .log.open .details {
		background: var(--activeBg);
		padding: 10px 0;
	} */

	span.closeDetails {
		font-weight: bold;
		color: var(--blue);
		text-transform: uppercase;
		margin-left: 10px;
		cursor: pointer;
	}

	span.closeDetails:before {
		display: inline-block;
		width: calc(100% - 360px);
		height: 1px;
		background: var(--blue);
		content: " ";
		position: relative;
		top: -4px;
		margin-right: 10px;
	}

	.details .errorLevel:before {
		content: "|";
		font-size: 30px;
		position: absolute;
		top: -7px;
		left: -6px;
		height: 23px;
		overflow: hidden;
	}

	.details .errorLevel {
		padding-left: 9px;
		position: relative;
	}

	@media screen and (min-width: 2000px) {
		ul.fields li {
			width: 21.75%;;
		}
	}

	@media screen and (max-width: 1500px) {
		ul.fields li {
			width: 47%;
		}
	}

</style>