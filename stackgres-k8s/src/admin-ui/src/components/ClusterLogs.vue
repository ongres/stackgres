<template>
	<div id="sg-logs" v-if="loggedIn && isReady && !notFound">
		<div class="content noScroll">
			<div id="logs">
				<div class="toolbar">
					<div class="floatLeft">
						<div class="filter filters columns" :data-filtered="isFiltered">
							<span class="toggle">FILTER</span>

							<ul class="options">
								<li class="columns">
									<span class="title">Type</span>
									<ul class="options">
										<li class="column">
											<label for="logTypePg">
												<span>Postgres</span>
												<input v-model="filters.logType.pg" type="checkbox" id="logTypePg"/>
											</label>
										</li>
										<li class="column">
											<label for="logTypePatroni">
												<span>Patroni</span>
												<input v-model="filters.logType.pa" type="checkbox" id="logTypePatroni"/>
											</label>
										</li>
									</ul>
								</li>

								<li class="columns">
									<span class="title">Role</span>
									<ul class="options">
										<li class="column">
											<label for="rolePrimary">
												<span>Primary</span>
												<input v-model="filters.role.primary" type="checkbox" id="rolePrimary"/>
											</label>
										</li>
										<li class="column">
											<label for="roleReplica">
												<span>Replica</span>
												<input v-model="filters.role.replica" type="checkbox" id="roleReplica"/>
											</label>
										</li>
										<li class="column">
											<label for="rolePromoted">
												<span>Promoted</span>
												<input v-model="filters.role.promoted" type="checkbox" id="rolePromoted"/>
											</label>
										</li>
										<li class="column">
											<label for="roleDemoted">
												<span>Demoted</span>
												<input v-model="filters.role.demoted" type="checkbox" id="roleDemoted"/>
											</label>
										</li>
										<li class="column">
											<label for="roleUninitialized">
												<span>Uninitialized</span>
												<input v-model="filters.role.uninitialized" type="checkbox" id="roleUninitialized"/>
											</label>
										</li>
										<li class="column">
											<label for="roleStandby">
												<span>Standby</span>
												<input v-model="filters.role.standby" type="checkbox" id="roleStandby"/>
											</label>
										</li>
									</ul>
								</li>

								<li class="columns">
									<span class="title">Error Level</span>
									<ul class="options">
										<li class="column">
											<label for="errorLevelPanic">
												<span>PANIC</span>
												<input v-model="filters.errorLevel.PANIC" type="checkbox" id="errorLevelPanic"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelCritical">
												<span>CRITICAL</span>
												<input v-model="filters.errorLevel.CRITICAL" type="checkbox" id="errorLevelCritical"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelFatal">
												<span>FATAL</span>
												<input v-model="filters.errorLevel.FATAL" type="checkbox" id="errorLevelFatal"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelLog">
												<span>LOG</span>
												<input v-model="filters.errorLevel.LOG" type="checkbox" id="errorLevelLog"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelError">
												<span>ERROR</span>
												<input v-model="filters.errorLevel.ERROR" type="checkbox" id="errorLevelError"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelWarning">
												<span>WARNING</span>
												<input v-model="filters.errorLevel.WARNING" type="checkbox" id="errorLevelWarning"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelNotice">
												<span>NOTICE</span>
												<input v-model="filters.errorLevel.NOTICE" type="checkbox" id="errorLevelNotice"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelInfo">
												<span>INFO</span>
												<input v-model="filters.errorLevel.INFO" type="checkbox" id="errorLevelInfo"/>
											</label>
										</li>
										<li class="column">
											<label for="errorLevelDebug">
												<span>DEBUG</span>
												<input v-model="filters.errorLevel.DEBUG" type="checkbox" id="errorLevelDebug"/>
											</label>
										</li>
									</ul>
								</li>
								<li class="textFilter">
									<span class="title">Pod Name</span>
									<input v-model="textFilters.podName" class="search" placeholder="Search pod name...">
								</li>
								<li class="textFilter">
									<span class="title">User Name</span>
									<input v-model="textFilters.userName" class="search" placeholder="Search user name...">
								</li>
								<li class="textFilter">
									<span class="title">Database Name</span>
									<input v-model="textFilters.databaseName" class="search" placeholder="Search database name...">
									<!-- <span class="btn clear border" @click="textFilters.databaseName = ''" v-if="textFilters.databaseName.length">×</span> -->
								</li>
								<li>
									<hr>
									<a class="btn" @click="getLogs(false)">APPLY</a> <a class="btn clear border" @click="clearFilters('filters')">RESET</a>
								</li>
							</ul>
						</div>

						<div class="filter columns" :data-filtered="(typeof (Object.keys(showColumns).find(k => !showColumns[k])) !== 'undefined')">
							<span class="toggle">COLUMNS</span>

							<ul class="options">
								<li class="column">
									<label for="viewLogType">
										<span>Log Type</span>
										<input v-model="showColumns.logType" type="checkbox" id="viewLogType"/>
									</label>
								</li>
								<li class="column">
									<label for="viewPodName">
										<span>Pod Name</span>
										<input v-model="showColumns.podName" type="checkbox" id="viewPodName"/>
									</label>
								</li>
								<li class="column">
									<label for="viewRole">
										<span>Role</span>
										<input v-model="showColumns.role" type="checkbox" id="viewRole"/>
									</label>
								</li>
								<li class="column">
									<label for="viewLogMessage">
										<span>Message</span>
										<input v-model="showColumns.message" type="checkbox" id="viewLogMessage"/>
									</label>
								</li>
								<li class="column">
									<label for="viewUserName">
										<span>User</span>
										<input v-model="showColumns.userName" type="checkbox" id="viewUserName"/>
									</label>
								</li>
								<li class="column">
									<label for="viewDatabaseName">
										<span>Database</span>
										<input v-model="showColumns.databaseName" type="checkbox" id="viewDatabaseName"/>
									</label>
								</li>
								<li class="column">
									<label for="viewProcessId">
										<span>Process ID</span>
										<input v-model="showColumns.processId" type="checkbox" id="viewProcessId"/>
									</label>
								</li>
								<li class="column">
									<label for="viewConnectionFrom">
										<span>Connection From</span>
										<input v-model="showColumns.connectionFrom" type="checkbox" id="viewConnectionFrom"/>
									</label>
								</li>
								<li class="column">
									<label for="viewApplicationName">
										<span>Application</span>
										<input v-model="showColumns.applicationName" type="checkbox" id="viewApplicationName"/>
									</label>
								</li>
							</ul>
						</div>

						<div class="searchBar" :class="(textFilters.text.length ? 'filtered' : '')" >
							<input id="keyword" v-model="textFilters.text" class="search" placeholder="Search text..." autocomplete="off">
							<button class="btn" @click="getLogs()">APPLY</button>
							<button @click="textFilters.text = ''" class="btn clear border keyword" v-if="textFilters.text.length">CLEAR</button>
						</div>
					</div>

					<div id="dateFilters" class="floatRight" :data-filtered="( (datePicker.length > 0) && !Object.keys(timeRange).length )">
						<ul id="timeRange">
							<li v-for="time in timeRangeOptions" :class="( ((timeRange.number == time.number) && (timeRange.unit == time.unit) ) && 'active')">
								<button class="plain" @click="setTimeRange(time.number, time.unit)">{{ time.number + time.unit }}</button>
							</li>
						</ul>
						<span class="toggle date">
							<svg xmlns="http://www.w3.org/2000/svg" width="15.002" height="16.503"><g fill="none" stroke="#7a7b85" stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"><path d="M2.25 2.251h10.5a1.5 1.5 0 0 1 1.5 1.5v10.5a1.5 1.5 0 0 1-1.5 1.5H2.25a1.5 1.5 0 0 1-1.5-1.5v-10.5a1.5 1.5 0 0 1 1.5-1.5ZM10.501.75v3M4.5.75v3M.75 6.751h13.5"/></g></svg>
							<input v-model="datePicker" id="datePicker" autocomplete="off" @focus="initDatePicker()">
						</span>
					</div>
				</div>

				<div class="logsContainer monoFont">
					<ul class="legend">
						<li class="field errorLevel" v-if="showColumns.errorLevel">
							<span title="Error Level">Error Level</span>
							<span class="helpTooltip" data-tooltip='Error Level of the log entry. Available levels are:<br/><ul class="monoFont errorLevelLegend upper"><li class="debug">Debug</li><li class="info">Info</li><li class="notice">Notice</li><li class="warning">Warning</li><li class="error">Error</li><li class="log">Log</li><li class="fatal">Fatal</li><li class="critical">Critical</li><li class="panic">Panic</li></ul>'></span>
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
					<div class="records monoFont">
						<template v-if="noData">
							<span class="no-data scroller">
								No records matched your search terms
							</span>
						</template>
						<template v-else>
							<DynamicScroller
								:items="logs"
								:min-item-size="15"
								:buffer="records * 20"
								class="scroller"
								key-field="logTimeIndex"
							>
								<template v-slot="{ item, index, active }">
								<DynamicScrollerItem
									:item="item"
									:active="active"
									:size-dependencies="[
										item.message,
									]"
									:data-index="index"
								>
									<div class="log" :class="( (index == currentLog) ? 'open' : '' )" :set="log = item">
										<span class="errorLevel" :class="log.errorLevel.toLowerCase()" :title="'Error Level: ' + log.errorLevel" v-if="showColumns.errorLevel"></span>
										<div v-if="index != currentLog" class="base"  @click="currentLog = index">
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
													<span class="field value label errorLevel" :class="log.errorLevel.toLowerCase()"><span>{{ log.errorLevel }}</span></span>
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
													<span class="field value timestamp">
														<span class='date'>
															{{ log.sessionStartTime | formatTimestamp('date') }}
														</span>
														<span class='time'>{{ log.sessionStartTime | formatTimestamp('time') }}</span><span class='ms'>{{ log.sessionStartTime | formatTimestamp('ms') }}</span>
														<span class='tzOffset'>{{ showTzOffset() }}</span>
													</span>
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
								</DynamicScrollerItem>
								</template>
							</DynamicScroller>
						</template>
					</div>
				</div>
				<div class="logOptions form">
					<small>Log records are loaded <strong>{{ liveMonitoring ? 'automatically' : 'on scroll' }}</strong></small>
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
	import moment from 'moment'

	import { RecycleScroller } from 'vue-virtual-scroller'
	import { DynamicScroller } from 'vue-virtual-scroller'
	import { DynamicScrollerItem } from 'vue-virtual-scroller'
	import 'vue-virtual-scroller/dist/vue-virtual-scroller.css'

    export default {
        name: 'ClusterLogs',

		mixins: [mixin],

		components: {
			RecycleScroller: RecycleScroller,
			DynamicScroller: DynamicScroller,
			DynamicScrollerItem: DynamicScrollerItem
		},

		data: function() {

			return {
				logs: [],
				records: 50,
				fetching: false,
				pooling: null,
				lastCall: '',
				currentLog: -1,
				noData: false,
				liveMonitoring: true,
				scrollAwait: null,
				lastScroll: 0,
				filters: {
					logType: {
						pg: true,
						pa: true
					},
					role: {
						primary: true,
						replica: true,
						promoted: true,
						demoted: true,
						uninitialized: true,
						standby: true
					},
					errorLevel: {
						PANIC: true,
						CRITICAL: true,
						FATAL: true,
						LOG: true,
						ERROR: true,
						WARNING: true,
						NOTICE: true,
						INFO: true,
						DEBUG: true,
					},
				},
				textFilters: {
					text: '',
					podName: '',
					userName: '',
					databaseName: ''
				},
				datePicker: '',
				dateStart: '',
				dateEnd: '',
				timeRange: {},
				timeRangeOptions: [
					{
						number: '15',
						unit: 'm'
					},
					{
						number: '30',
						unit: 'm'
					},
					{
						number: '1',
						unit: 'h'
					},
					{
						number: '2',
						unit: 'h'
					},
					{
						number: '6',
						unit: 'h'
					},
					{
						number: '24',
						unit: 'h'
					},
					{
						number: '1',
						unit: 'w'
					},
					{
						number: '1',
						unit: 'M'
					}
				],
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
				return store.state.sgclusters
			},
			
			grafanaEmbedded() {
				var grafana = false;
				const vm = this;
				
				store.state.sgclusters.forEach(function( c ){
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
			},

			isFiltered() {

				const vc = this;

				let byLogType = (typeof (Object.keys(vc.filters.logType).find( k => !vc.filters.logType[k])) !== 'undefined');
				let byErrorLevel = (typeof (Object.keys(vc.filters.errorLevel).find( k => !vc.filters.errorLevel[k])) !== 'undefined');
				let byTextFilters = (typeof (Object.keys(vc.filters.errorLevel).find( k => vc.textFilters.length)) !== 'undefined');

				return (byLogType || byErrorLevel || byTextFilters)
			},

		},
		mounted: function() {

			// Load datepicker
			require('daterangepicker');
						
			const vc = this;

			$(document).ready(function(){

				vc.records = parseInt($('.records').height() / 15);
				vc.getLogs();

				$(document).on('keyup', 'input.search', function(e){
					if (e.keyCode === 13)
						vc.getLogs();
				});

			});

			document.addEventListener('scroll', function (event) {
				if (!vc.liveMonitoring && event.target.className.includes('scroller') ) {
					vc.handleScroll()
				}
			}, true);

		},
		methods: {

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
				const vc = this;

				if(section == 'filters') {
					Object.keys(vc.filters).forEach(function(filter){
						Object.keys(vc.filters[filter]).forEach(function(prop){
							vc.filters[filter][prop] = true
						})
					})
					
					Object.keys(vc.textFilters).forEach(function(filter){
						vc.textFilters[filter] = ''
					})

					$('.filters.open').removeClass('open')

				} else if (section == 'keyword') {
					this.text = '';
					$('#keyword').removeClass('active')

					$('.searchBar .clear').fadeOut()
				} else {
					this[section] = '';
				}
				
				vc.getLogs(false);
				vc.currentLog = -1;
				vc.scrollToBottom();
			},

			getLogs(append = false, byDate = false) {

				if(store.state.notFound) {
					return false;
				}

				const vc = this;

				$('.records').addClass('loading');

				if(!append) {
					clearTimeout(vc.pooling);
					$('.filter.open').removeClass('open');
				}

				let params = '?sort=asc&records='+this.records;

				if(vc.datePicker.length) {
					vc.liveMonitoring = false;
				}
				
				if( Object.keys(vc.filters.logType).find( k => !vc.filters.logType[k] ) ) {

					// If no logTypes selected, clear logs
					if(typeof Object.keys(vc.filters.logType).find( k => vc.filters.logType[k] ) == 'undefined' ) {
						vc.logs = [];
						vc.noData = true;
						return false
					}

					Object.keys(vc.filters.logType).forEach(function(value){
						if(vc.filters.logType[value]) {
							params += '&logType=' + value;
						}
					})

				}

				if( Object.keys(vc.filters.errorLevel).find( k => !vc.filters.errorLevel[k] ) ) {

					// If no errorLevel selected, clear logs
					if(typeof Object.keys(vc.filters.errorLevel).find( k => vc.filters.errorLevel[k] ) == 'undefined' ) {
						vc.logs = [];
						vc.noData = true;
						return false
					}

					Object.keys(vc.filters.errorLevel).forEach(function(value){
						if(vc.filters.errorLevel[value]) {
							params += '&errorLevel=' + value;
						}
					})

				}

				Object.keys(vc.textFilters).forEach( function(filter) {
					if( vc.textFilters[filter].length ) {
						params += '&' + filter + '=' + vc.textFilters[filter];
					}
				})
				
				if(this.dateStart.length && byDate) {
					params += '&from='+this.dateStart;
				}
				
				if(this.dateEnd.length && byDate) {
					params += '&to='+this.dateEnd;
				}
				
				let thisCall = '/stackgres/namespaces/' + this.$route.params.namespace + '/sgclusters/' + this.$route.params.name + '/logs' + params;
				vc.fetching = true;

				axios
				.get(thisCall)
				.then( function(response){

					if( vc.liveMonitoring && (!append || ( $('.scroller')[0].scrollTop == ( $('.scroller')[0].scrollHeight - $('.scroller')[0].clientHeight ) ) ) ) {
						vc.scrollToBottom();
					}

					if(append) {
						vc.logs = vc.logs.concat(response.data);
					} else {
						vc.logs = response.data;
						vc.lastScroll = 0;
						$('.scroller')[0].scrollTop = 0;
					}

					$('.records').removeClass('loading');
					vc.fetching = false;
					
				}).catch(function(err) {
					vc.notify(
						{
						title: 'Error',
						detail: 'There was an error while trying to fetch the information from the API, please refresh the window and try again.'
						},
						'error'
					);

					vc.logs = [];
					console.log(err);
					vc.checkAuthError(err);

					$('.records').removeClass('loading');
					vc.fetching = false;
				});

				if(vc.liveMonitoring) {
					vc.pooling = setTimeout(() => {
						if(!vc.fetching && vc.liveMonitoring) {
							if(vc.logs.length) {
								let ltime = vc.logs[vc.logs.length-1].logTime;
								let lindex = vc.logs[vc.logs.length-1].logTimeIndex;
								vc.dateStart = ltime+','+lindex;
							}
							vc.getLogs(true, true);
						}
					}, 3000);
				} else if (vc.pooling) {
					clearInterval(vc.pooling);
					vc.pooling = null;
				}

			},
			
			scrollToBottom() {
				const vc = this;

				vc.scrollAwait = setInterval(function() {
					if 	( $('.scroller')[0].scrollTop != ( $('.scroller')[0].scrollHeight - $('.scroller')[0].clientHeight ) &&
						(!$('.log.open').length || !isElementInViewport($('.log.open')[0]) ) 
					) {
						$('.scroller')[0].scrollTop = $('.scroller')[0].scrollHeight + $('.scroller')[0].clientHeight;
					} else {
						clearInterval(vc.scrollAwait)
						vc.scrollAwait = null;
					}
				},100);
				
			},

			handleScroll() {
				let vc = this;

				if( !vc.fetching &&
					!vc.scrollAwait &&
					!$('.records').hasClass('loading') && 
					($('.scroller').scrollTop() > vc.lastScroll) &&
					(( ($('.scroller').scrollTop() + $('.scroller').innerHeight() >= ($('.scroller')[0].scrollHeight - 500) )) ) ) {

					let ltime = vc.logs[vc.logs.length-1].logTime;
					let lindex = vc.logs[vc.logs.length-1].logTimeIndex;
					vc.dateStart = ltime+','+lindex;
					vc.lastScroll = $('.scroller').scrollTop();
					vc.getLogs(true, true);
				}

			},

			initDatePicker() {

				if(!$('.daterangepicker').length) {

					const vc = this;
						
					$('#datePicker').daterangepicker({
						"parentEl": "#logs",
						"autoApply": true,
						"timePicker": true,
						"timePicker24Hour": true,
						"timePickerSeconds": true,
						"opens": "right",
						"maxDate": new Date(),
						locale: {
							cancelLabel: "RESET"
						}
					}, function(start, end, label) {

						if(store.state.timezone == 'utc') {
							vc.dateStart = start.format('YYYY-MM-DDTHH:mm:ss') + 'Z'
							vc.dateEnd = end.format('YYYY-MM-DDTHH:mm:ss') + 'Z'
						} else {
							vc.dateStart = moment.utc(start).format('YYYY-MM-DDTHH:mm:ss') + 'Z'
							vc.dateEnd = moment.utc(end).format('YYYY-MM-DDTHH:mm:ss') + 'Z'
						}
						vc.datePicker = vc.dateStart+' / '+vc.dateEnd;
						vc.timeRange = {};
						vc.getLogs(false, true);

					});

					$('#datePicker').on('show.daterangepicker', function(ev, picker) {
						if(!vc.datePicker.length) {
							$('.daterangepicker td.active').addClass('deactivate')
							$('.daterangepicker td.in-range').removeClass('in-range')
						}

						$('#datePicker').parent().addClass('open');
					});

					$('#datePicker').on('hide.daterangepicker', function(ev, picker) {
						$('#datePicker').parent().removeClass('open');

						if(vc.datePicker.length) {
							$('.daterangepicker td.deactivate').removeClass('deactivate');
						}
					});

					$('#datePicker').on('cancel.daterangepicker', function(ev, picker) {
						vc.clearDateFilters();
						$('#datePicker').parent().removeClass('open');
					});
				}

			},

			clearDateFilters() {
				const vc = this;

				vc.datePicker = '';
				vc.dateStart = '';
				vc.dateEnd = '';
				vc.liveMonitoring = true;
				vc.timeRange = {};

				vc.getLogs();
			},

			setTimeRange(number, unit) {
				const vc = this;

				if(!$('.daterangepicker').length) {
					vc.initDatePicker();
				}
				
				if( !vc.timeRange.hasOwnProperty('unit') || (vc.timeRange.hasOwnProperty('unit') && ( (vc.timeRange.number + vc.timeRange.unit) != (number + unit) ) ) ) {

					let now = moment();
					vc.dateStart = moment.utc(now).subtract(number, unit).format('YYYY-MM-DDTHH:mm:ss') + 'Z';
					vc.dateEnd = moment.utc(now).format('YYYY-MM-DDTHH:mm:ss') + 'Z';
					vc.timeRange = {'number': number, 'unit': unit};
					vc.datePicker = vc.dateStart+' / '+vc.dateEnd;

					$('#datePicker').data('daterangepicker').setEndDate(now);
					$('#datePicker').data('daterangepicker').setStartDate(now.subtract(number, unit));
					vc.getLogs(false, true);

				} else {
					vc.clearDateFilters();
				}

			}

		},
		beforeDestroy: function() {
			$('.daterangepicker').remove()
			clearInterval(this.pooling);
		}
	}

	function isElementInViewport(el) {
		var rect = el.getBoundingClientRect();

    	return rect.bottom > 0 &&
			rect.right > 0 &&
			rect.left < (window.innerWidth || document.documentElement.clientWidth) /* or $(window).width() */ &&
			rect.top < (window.innerHeight || document.documentElement.clientHeight) /* or $(window).height() */;
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

	.records:after {
		opacity: 0;
		display: block;
		transition: opacity .2s ease-in;
	}

	.records.loading:after {
		border-radius: 100%;
		content: " ";
		position: fixed;
		bottom: 100px;
		right: 55px;
		width: 35px;
		height: 35px;
		background: url('/assets/img/loader.gif') center center no-repeat rgba(0,0,0,.05);
		background-size: 70%;
		opacity: 1;
		transition: opacity .4s ease-in;
	}

	.darkmode .records.loading:after {
		background: url('/assets/img/loader.gif') center center no-repeat rgba(0,0,0,.5);
		background-size: 70%;
	}

	.records {
		padding-top: 15px;
		height: calc(100vh - 430px);
		max-height: calc(100vh - 430px);
		overflow: auto;
	}

	div.log {
		font-size: 14px;
		padding: 2px 0 1px 10px;
		display: inline-block;
		width: 100%;
		position: relative;
	}

	div.log:before {
		content: "▸";
		position: absolute;
		margin-right: 11px;
		font-size: 11px;
		left: 0;
	}

	div.log.open:before {
		content: "▾"
	}

	div.log:not(.open) .base:hover {
		background: var(--activeBg);
		cursor: pointer;
	}

	.log .base {
		display: inline-block;
		padding-left: 10px;
		line-height: 1.4;
    	width: calc(100% - 10px);
	}

	.log .base span.field {
		margin-right: 10px;
		word-break: break-word;
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
		height: calc(100% - 4px);
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
		width: 31%;
		padding: 7px 10px;
		border-bottom: 1px solid var(--borderColor);
		margin: 0 1%;
	}
	
	ul.fields li.logMessage {
		width: 97%;
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
	
	span.closeDetails {
		font-weight: bold;
		color: var(--blue);
		text-transform: uppercase;
		margin-left: 10px;
		cursor: pointer;
		float: right;
		margin-right: 2%;
		display: block;
		width: calc(100% - 250px);
		text-align: right;
		position: relative;
	}
	
	span.closeDetails:hover {
		color: var(--lBlue);
	}

	span.closeDetails:before {
		display: inline-block;
		width: calc(100% - 98px);
		height: 1px;
		background: var(--blue);
		content: " ";
		position: absolute;
		top: 8px;
		right: 100px;
	}

	span.closeDetails:hover:before {
		background: var(--lBlue);
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

	#logs .daterangepicker {
		top: 273px !important;
		right: 40px !important;
		left: auto !important;
	}

	@media screen and (min-width: 2500px) {
		ul.fields li {
			width: 21.75%;;
		}

		span.closeDetails {
			width: calc(100% - 255px);
		}
	}

	@media screen and (max-width: 1800px) {
		ul.fields li {
			width: 47%;
		}

		ul.fields li.logMessage {
			width: 96%;
		}

		span.closeDetails {
			margin-right: 3%;
			width: calc(99% - 245px);
		}
	}
</style>

<style scoped>
	.logOptions {
		padding: 10px 10px 0;
		border-top: 1px solid var(--borderColor);
		margin-top: 10px;
		width: 100%;
    	max-width: 100%;
		text-align: center;
		font-size: 90%;
	}

	.logOptions select {
		width: auto;
		display: inline-block;
		height: auto;
		font-size: 85%;
		padding: 9px 25px 9px 10px;
		cursor: pointer;
		position: relative;
		top: 0;
		margin: 0 5px;
		margin-bottom: 0;
		background-position: 90% center;
	}

	.logOptions .btn {
		font-size: 80%;
		float: right;
		margin: 0;
	}

	.scroller {
		height: 100%;
	}

	#timeRange {
		list-style: none;
    	display: inline-block;
	}

	#timeRange li {
		display: inline-block;
		margin-right: -1px;
	}

	#timeRange li:hover, #timeRange li.active {
		position: relative;
		z-index: 2;
	}

	#timeRange button {
		padding: 0 12px;
		border: 1px solid var(--borderColor);
    	border-radius: 0;
		font-weight: normal;
		color: var(--gray4);
		font-size: 12px;
		height: 30px;
		line-height: 30px;
		text-transform: none;
		width: 50px;
		text-align: center;
	}

	#timeRange li:first-child button {
		border-top-left-radius: 5px;
		border-bottom-left-radius: 5px;
	}

	#timeRange button:hover, #timeRange li.active button {
		color: var(--baseColor);
		border-color: var(--baseColor);
		background: rgba(0, 173, 181, .25);
	}

	.toggle.date {
		border-top-left-radius: 0;
		border-bottom-left-radius: 0;
		margin-left: 0;
		height: 30px;
	}

	.toggle.date svg {
		transform: translate(-4px, 3px) scale(.85);
	}

	.toggle.date:hover svg path, .toggle.date.open svg path, [data-filtered="true"] .toggle.date svg path {
		stroke: var(--baseColor);
	}

	ul.legend {
		position: sticky;
		top: 0;
		background: var(--bgColor);
		box-shadow: 0 10px 40px rgb(255 255 255 / 90%);
		z-index: 1;
		font-size: 14px;
		padding: 10px 0 0;
	}

	.darkmode ul.legend {
		box-shadow: 0 10px 60px rgb(23 23 23 / 90%);
	}

	ul.legend li {
		display: inline-block;
		margin: 5px 10px;
		padding: 0;
		border: 0;
	}

	ul.legend span.helpTooltip {
		margin-left: 5px;
	}

	ul.errorLevelLegend li {
		width: 50%;
		float: left;
		text-transform: uppercase;
	}
</style>