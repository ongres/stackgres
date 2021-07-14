<template>
	<div id="sg-logs" v-if="loggedIn && isReady && !notFound">
		<header>
			<ul class="breadcrumbs">
				<li class="namespace">
					<svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
					<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="Namespace Clusters Overview">{{ $route.params.namespace }}</router-link>
				</li>
				<li>
					<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
					<router-link :to="'/' + $route.params.namespace + '/sgclusters'" title="Namespace Clusters Overview">SGClusters</router-link>
				</li>
				<li>
					<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name" title="Status">{{ $route.params.name }}</router-link>
				</li>
				<li>
					Logs
				</li>
			</ul>

			<div class="actions">
			<a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgcluster/" target="_blank" title="SGCluster Documentation">SGCluster Documentation</a>
				<div>
					<a v-if="iCan('create','sgclusters',$route.params.namespace)" class="cloneCRD" @click="cloneCRD('SGCluster', $route.params.namespace, $route.params.name)">Clone Cluster Configuration</a>
					<router-link v-if="iCan('patch','sgclusters',$route.params.namespace)" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/edit'">Edit Cluster</router-link>
					<a v-if="iCan('delete','sgclusters',$route.params.namespace)" @click="deleteCRD('sgcluster', $route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/sgclusters')" :class="$route.params.namespace + '/sgclusters'">Delete Cluster</a>
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
				<li v-if="iCan('list','sgdistributedlogs',$route.params.namespace)">
					<router-link :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/logs'" title="Distributed Logs" class="logs">Logs</router-link>
				</li>
				<li v-if="grafanaEmbedded">
					<router-link id="grafana-btn" :to="'/' + $route.params.namespace + '/sgcluster/' + $route.params.name + '/monitor'" title="Grafana Dashboard" class="grafana">Monitoring</router-link>
				</li>
			</ul>
		</header>

		<div class="content noScroll">
			<div id="logs">
				<div class="toolbar">
					<div class="searchBar" :class="(textFilters.text.length ? 'filtered' : '')" >
						<input id="keyword" v-model="textFilters.text" class="search" placeholder="Search text..." autocomplete="off">
						<a @click="getLogs()" class="btn">APPLY</a>
						<a @click="textFilters.text = ''" class="btn clear border keyword" v-if="textFilters.text.length">RESET</a>
					</div>

					<div class="filter" :data-filtered="datePicker.length > 0">
						<span class="toggle date">DATE RANGE <input v-model="datePicker" id="datePicker" autocomplete="off" @focus="initDatePicker()"></span>
					</div>

					<div class="filter filters columns" :data-filtered="isFiltered">
						<span class="toggle">FILTER</span>

						<ul class="options">
							<!-- <span class="title">Filter logs by</span> -->
							<li class="columns">		
								<ul class="options">
									<li class="column">
										<strong class="label">Show logs</strong>
										<ul class="columns border">
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
									<li class="column textFilter">
										<strong class="label">Pod Name</strong>
										<input v-model="textFilters.podName" class="search" placeholder="Search pod name...">
									</li>
								</ul>
							</li>

							<ul class="options columns">
								<li class="column">
									<span class="title">Postgres Filters</span>

									<ul>
										<li class="field">
											<strong class="label">Log Level</strong>
											<div class="range-slider">
												<ul class="steps">
													<li v-for="(level, index) in filters.logLevel.postgres.levels" :class="[level.toLowerCase(), (filters.logLevel.postgres.index == index) ? 'show' : '']"  :style="'left: ' + ( (filters.logLevel.postgres.levels.length == (index + 1)) ? 'calc(100% - 4px)' : ( (100 / (filters.logLevel.postgres.levels.length - 1)) * index) + '%' )">
														<i></i>
														<span>{{ level }}</span>
													</li>
												</ul>

												<input class="range-slider__range" type="range" min="0" :max="filters.logLevel.postgres.levels.length - 1" v-model="filters.logLevel.postgres.index">
											</div>
										</li>

										<li class="field">
											<strong class="label">Role</strong>
											<ul class="columns border">
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
											</ul>
										</li>

										<li class="textFilter field">
											<strong class="label">User Name</strong>
											<input v-model="textFilters.userName" class="search" placeholder="Search user name...">
										</li>

										<li class="textFilter field">									
											<strong class="label">Database Name</strong>
											<input v-model="textFilters.databaseName" class="search" placeholder="Search database name...">
										</li>
									</ul>
								</li>
								<li class="column" :class="!filters.logType.patroni ? 'disabled' : ''">
									<span class="title">Patroni Filters</span>

									<ul>
										<li class="field">
											<strong class="label">Log Level</strong>
											<div class="range-slider">
												<ul class="steps">
													<li v-for="(level, index) in filters.logLevel.patroni.levels" :class="[level.toLowerCase(), (filters.logLevel.patroni.index == index) ? 'show' : '']"  :style="'left: ' + ( (filters.logLevel.patroni.levels.length == (index + 1)) ? 'calc(100% - 4px)' : ( (100 / (filters.logLevel.patroni.levels.length - 1)) * index) + '%' )">
														<i></i>
														<span>{{ level }}</span>
													</li>
												</ul>

												<input class="range-slider__range" type="range" min="0" :max="filters.logLevel.patroni.levels.length - 1" v-model="filters.logLevel.patroni.index" :disabled="!filters.logType.patroni ? 'disabled' : ''">
											</div>
										</li>

										<li class="field">
											<strong class="label">Role</strong>
											<ul class="columns border">
												<li class="column">
													<label for="rolePrimary">
														<span>Primary</span>
														<input v-model="filters.role.primary" type="checkbox" id="rolePrimary" :disabled="!filters.logType.patroni ? 'disabled' : ''"/>
													</label>
												</li>
												<li class="column">
													<label for="roleReplica">
														<span>Replica</span>
														<input v-model="filters.role.replica" type="checkbox" id="roleReplica" :disabled="!filters.logType.patroni ? 'disabled' : ''"/>
													</label>
												</li>
												<li class="column">
													<label for="rolePromoted">
														<span>Promoted</span>
														<input v-model="filters.role.promoted" type="checkbox" id="rolePromoted" :disabled="!filters.logType.patroni ? 'disabled' : ''"/>
													</label>
												</li>
												<li class="column">
													<label for="roleDemoted">
														<span>Demoted</span>
														<input v-model="filters.role.demoted" type="checkbox" id="roleDemoted" :disabled="!filters.logType.patroni ? 'disabled' : ''"/>
													</label>
												</li>
												<li class="column">
													<label for="roleUninitialized">
														<span>Uninitialized</span>
														<input v-model="filters.role.uninitialized" type="checkbox" id="roleUninitialized" :disabled="!filters.logType.patroni ? 'disabled' : ''"/>
													</label>
												</li>
												<li class="column">
													<label for="roleStandby">
														<span>Standby</span>
														<input v-model="filters.role.standby" type="checkbox" id="roleStandby" :disabled="!filters.logType.patroni ? 'disabled' : ''"/>
													</label>
												</li>
											</ul>
										</li>
									</ul>
								</li>
							</ul>

							<li>
								<hr>
								<a class="btn" @click="getLogs(false)">APPLY</a> <a class="btn clear border" @click="clearFilters('filters')">RESET</a>
							</li>
						</ul>
					</div>

					<div class="filter columns" :data-filtered="(typeof (Object.keys(showColumns).find(k => !showColumns[k])) !== 'undefined')">
						<span class="toggle">VISIBLE FIELDS</span>

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

				<div class="logsContainer monoFont">
					<ul class="legend">
						<li class="field logLevel" v-if="showColumns.logLevel">
							<span title="Error Level">Error Level</span>
							<span class="helpTooltip" data-tooltip='Error Level of the log entry. Available levels are:<br/><ul class="monoFont logLevelLegend upper"><li class="debug">Debug</li><li class="info">Info</li><li class="notice">Notice</li><li class="warning">Warning</li><li class="error">Error</li><li class="log">Log</li><li class="fatal">Fatal</li><li class="critical">Critical</li><li class="panic">Panic</li></ul>'></span>
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
										<span class="logLevel" :class="log.errorLevel.toLowerCase()" :title="'Error Level: ' + log.errorLevel" v-if="showColumns.logLevel"></span>
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
													<span class="field value label logLevel" :class="log.errorLevel.toLowerCase()"><span>{{ log.errorLevel }}</span></span>
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
					Loading Method:
					<select v-model="loadingMethod" @change="getLogs(true, true)">
						<option value="live">Live Reload</option>
						<option value="scroll">Load on Scroll</option>
					</select>

					<button @click="scrollToBottom()" class="btn border">Scroll to Bottom</button>
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
        name: 'Logs',

		mixins: [mixin],

		components: {
			RecycleScroller: RecycleScroller,
			DynamicScroller: DynamicScroller,
			DynamicScrollerItem: DynamicScrollerItem
		},

		data: function() {

			return {
				logs: [],
				currentSortDir: 'desc',
				records: 50,
				fetching: false,
				pooling: null,
				lastCall: '',
				currentLog: -1,
				noData: false,
				loadingMethod: 'live',
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
					logLevel: {
						postgres: {
							index: 6,
							levels: ["PANIC", "FATAL", "LOG", "ERROR", "WARNING", "NOTICE", "INFO", "DEBUG"]
						},
						patroni: {
							index: 3,
							levels: ["CRITICAL", "ERROR", "WARNING", "INFO", "DEBUG"]
						}
					}
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
				showColumns: {
					logLevel: true,
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
			},

			isFiltered() {

				const vc = this;

				let byLogType = (typeof (Object.keys(vc.filters.logType).find( k => !vc.filters.logType[k])) !== 'undefined');
				let byTextFilters = (typeof (Object.keys(vc.textFilters).find( k => vc.textFilters[k].length)) !== 'undefined');

				return (byLogType || byTextFilters)
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
				
				$(document).on('click', '#datePicker', function(){
					$(this).parent().toggleClass('open');
				});

			});

			document.addEventListener('scroll', function (event) {
				if (event.target.className.includes('scroller') && (vc.loadingMethod == 'scroll') ) {
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
						if($('.filters .options .active').length || $('.filters .options .search').val().length || this.role.length )
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

				const vc = this;

				$('.records').addClass('loading');

				if(!append) {
					vc.currentSortDir = 'desc'
					clearTimeout(vc.pooling);
					$('.filter.open').removeClass('open');
				}

				let params = '';

				params += '?records='+this.records;
				params += vc.datePicker.length ? '&sort=asc' : '&sort='+this.currentSortDir;
				
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

				/* if( Object.keys(vc.filters.errorLevel).find( k => !vc.filters.errorLevel[k] ) ) {

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

				} */

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
				
				let thisCall = '/stackgres/sgcluster/logs/'+this.$route.params.namespace+'/'+this.$route.params.name+params;
				vc.fetching = true;

				axios
				.get(thisCall)
				.then( function(response){

					if( (vc.currentSortDir == 'desc') && !vc.logs.length) {
						response.data.reverse();
						vc.currentSortDir = 'asc';
					}

					if( ( vc.loadingMethod == 'live') && (!append || ( $('.scroller')[0].scrollTop == ( $('.scroller')[0].scrollHeight - $('.scroller')[0].clientHeight ) ) ) ) {
						vc.scrollToBottom();
					}

					if(append) {
						vc.logs = vc.logs.concat(response.data);
					} else {
						vc.logs = response.data;
						vc.lastScroll = 0;
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

				if(vc.loadingMethod == 'live') {
					vc.pooling = setTimeout(() => {
						if(!vc.fetching && (vc.loadingMethod == 'live')) {
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

			setTime(time) {

				const vc = this;

				switch(time) {

					case '1d':
						if(vc.currentSortDir == 'asc') {
							vc.dateStart = vc.logs[0].logTime + ',' + vc.logs[0].logTimeIndex;

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
			
			scrollToBottom() {
				const vc = this;

				vc.scrollAwait = setInterval(function() {
					if ( $('.scroller')[0].scrollTop != ( $('.scroller')[0].scrollHeight - $('.scroller')[0].clientHeight ) ) {
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
					(vc.loadingMethod == 'scroll') &&
					($('.scroller').scrollTop() > vc.lastScroll) &&
					(( ($('.scroller').scrollTop() + $('.scroller').innerHeight() >= ($('.scroller')[0].scrollHeight - 300) )) ) ) {

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
						"opens": "left",
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

						if(vc.datePicker.length)
							$('.daterangepicker td.deactivate').removeClass('deactivate')
					});

					$('#datePicker').on('cancel.daterangepicker', function(ev, picker) {
						vc.currentSortDir = 'desc';
						vc.datePicker = '';
						vc.dateStart = '';
						vc.dateEnd = '';

						vc.getLogs();
						$('#datePicker').parent().removeClass('open');
					});
				}

			}

		},
		beforeDestroy: function() {
			$('.daterangepicker').remove()
			clearInterval(this.pooling);
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

	ul.logLevelLegend li {
		width: 50%;
		float: left;
		text-transform: uppercase;
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
		bottom: 105px;
		right: 50px;
		width: 30px;
		height: 30px;
		background: url('/assets/img/loader.gif') center center no-repeat rgba(0,0,0,.05);
		background-size: 70%;
		opacity: 1;
		transition: opacity .4s ease-in;
	}

	.darkmode .records.loading:after {
		background: url('/assets/img/loader.gif') center center no-repeat rgba(0,0,0,.5);
		background-size: 70%;
	}

	#log > .daterangepicker {
		margin-top: 63px;
		margin-right: 0;
	}


	#log .calendar li span {
		position: relative;
	}

	.records {
		padding-top: 15px;
		height: calc(100vh - 430px);
		max-height: calc(100vh - 430px);
		overflow: auto;
	}

	div.log {
		font-size: 14px;
		padding: 4px 0 4px 10px;
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
		display: flex;
	}

	.log .base span.field {
		margin-left: 10px;
		flex: none;
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

	.logLevelLegend .not-set, .details .not-set {
		color: var(--not-set);
	}

	.logLevelLegend .debug, .details .debug, .range-slider .steps li.debug span {
		color: var(--debug);
	}

	.logLevelLegend .info, .details .info, .range-slider .steps li.info span {
		color: var(--info);
	}

	.logLevelLegend .notice, .details .notice, .range-slider .steps li.notice span {
		color: var(--notice);
	}

	.logLevelLegend .warning, .details .warning, .range-slider .steps li.warning span {
		color: var(--warning);
	}

	.logLevelLegend .error, .details .error, .range-slider .steps li.error span {
		color: var(--error);
	}

	.logLevelLegend .log, .details .log, .range-slider .steps li.log span {
		color: var(--log);
	}

	.logLevelLegend .fatal, .details .fatal, .range-slider .steps li.fatal span {
		color: var(--fatal);
	}

	.logLevelLegend .critical, .details .critical, .range-slider .steps li.critical span {
		color: var(--critical);
	}

	.logLevelLegend .panic, .details .panic, .range-slider .steps li.panic span {
		color: var(--panic);
	}

	.log > .logLevel.not-set{
		background: var(--not-set);
	}

	.log > .logLevel.debug, .range-slider .steps li.debug i {
		background: var(--debug);
	}

	.log > .logLevel.info, .range-slider .steps li.info i {
		background: var(--info);
	}

	.log > .logLevel.notice, .range-slider .steps li.notice i {
		background: var(--notice);
	}

	.log > .logLevel.warning, .range-slider .steps li.warning i {
		background: var(--warning);
	}

	.log > .logLevel.error, .range-slider .steps li.error i {
		background: var(--error);
	}

	.log > .logLevel.log, .range-slider .steps li.log i {
		background: var(--log);
	}

	.log > .logLevel.fatal, .range-slider .steps li.fatal i {
		background: var(--fatal);
	}

	.log > .logLevel.critical, .range-slider .steps li.critical i {
		background: var(--critical);
	}

	.log > .logLevel.panic, .range-slider .steps li.panic i {
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

	.details .logLevel:before {
		content: "|";
		font-size: 30px;
		position: absolute;
		top: -7px;
		left: -6px;
		height: 23px;
		overflow: hidden;
	}

	.details .logLevel {
		padding-left: 9px;
		position: relative;
	}

	.logOptions {
		padding: 10px 10px 0;
		border-top: 1px solid var(--borderColor);
		margin-top: 10px;
		width: 100%;
    	max-width: 100%;
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

	.scroller {
		height: 100%;
	}

	/* Range Slider */
	.range-slider {
		width: 100%;
		position: relative;
		margin-top: 5px;
		margin-bottom: 10px;
	}

	.range-slider:before {
		content: " ";
		display: block;
		background: var(--activeBg);
		width: 100%;
		height: 4px;
		position: absolute;
		left: 0;
		top: 8px;
	}

	.range-slider__range {
		-webkit-appearance: none;
		position: relative;
		z-index: 10;
		width: 100%;
		height: 20px;
		border-radius: 5px;
		outline: none;
		padding: 0;
		margin: 0;
		background: transparent;
		cursor: pointer;
	}		

	.range-slider__range::-webkit-slider-thumb {
		-webkit-appearance: none;
		appearance: none;
		width: 20px;
		height: 20px;
		cursor: pointer;
		background: none;
		position: relative;
		z-index: 10;
		border: 0;
	}


	.range-slider__range::-moz-range-thumb {
		appearance: none;
		width: 20px;
		height: 20px;
		cursor: pointer;
		background: none;
		border: 0;
	}
	
	/* 
	.range-slider__range::-webkit-slider-thumb {
		-webkit-appearance: none;
		appearance: none;
		width: 10px;
		height: 20px;
		transform: translateX(-50%);
		border-radius: 50%;
		background: #2c3e50;
		cursor: pointer;
		-webkit-transition: background 0.15s ease-in-out;
		transition: background 0.15s ease-in-out;
	}

	.range-slider__range::-webkit-slider-thumb:hover {
		background: #1abc9c;
	}

	.range-slider__range:active::-webkit-slider-thumb {
		background: #1abc9c;
	}

	.range-slider__range::-moz-range-thumb {
		width: 20px;
		height: 20px;
		border: 0;
		border-radius: 50%;
		background: #2c3e50;
		cursor: pointer;
		-moz-transition: background 0.15s ease-in-out;
		transition: background 0.15s ease-in-out;
	}

	.range-slider__range::-moz-range-thumb:hover {
		background: #1abc9c;
	}

	.range-slider__range:active::-moz-range-thumb {
		background: #1abc9c;
	}

	.range-slider__range:focus::-webkit-slider-thumb {
		box-shadow: 0 0 0 3px #fff, 0 0 0 6px #1abc9c;
	}

	.range-slider__value {
		display: inline-block;
		position: absolute;
		color: #fff;
		line-height: 20px;
		text-align: center;
		border-radius: 3px;
		background: #2c3e50;
		padding: 5px 10px;
		margin-left: 8px;
	}

	.range-slider__value:after {
		position: absolute;
		top: 8px;
		left: -7px;
		width: 0;
		height: 0;
		border-top: 7px solid transparent;
		border-right: 7px solid #2c3e50;
		border-bottom: 7px solid transparent;
		content: "";
	} */

	::-moz-range-track {
		border: 0;
	}

	input::-moz-focus-inner,
	input::-moz-focus-outer {
		border: 0;
	}

	.range-slider .steps li {
		position: absolute;
		z-index: 1;
		width: 20px;
		height: 10px;
		transform: translateY(-12px);
		text-align: center;
	}

	.range-slider .steps li i {
		border-radius: 4px;
		display: inline-block;
		height: 100%;
		width: 4px;
		cursor: pointer;
	}		

	.range-slider .steps li span {
		display: none;
	}

	.range-slider .steps {
		position: absolute;
		width: 100%;
		top: 16px;
		font-size: 85%;
		font-weight: bold;
	}

	.range-slider .steps li.show i {
		height: 17px;
    	transform: translateY(-2px);
	}

	.range-slider .steps li.show span {
		display: inline-block;
		width: auto;
		margin-top: 5px;
		transform: translateX(calc(-50% + 2px));
	}

	ul.columns.border {
		padding: 7px 2px 0;
	}

	strong.label {
		display: block;
		margin-bottom: 5px;
	}

	.options li.field:not(:last-child) {
		margin-bottom: 15px;
	}

	ul.options > li.column:first-child {
		padding-right: 15px;
	}

	ul.options > li.column:last-child {
		padding-left: 15px;
	}

	.range-slider .steps li:first-child.show span {
		transform: none;
	}

	.range-slider .steps li:last-child.show span {
		transform: translateX(calc(-100% + 4px));
	}

</style>