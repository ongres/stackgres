<template>
    <div id="dbops" v-if="loggedIn && isReady">
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/' + $route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M17.1 20c-.6 0-1-.5-1-1 0-1.6-1.3-2.8-2.8-2.8H6.6c-1.6 0-2.8 1.3-2.8 2.8 0 .6-.5 1-1 1s-1-.5-1-1c0-2.7 2.2-4.8 4.8-4.8h6.7c2.7 0 4.8 2.2 4.8 4.8.1.5-.4 1-1 1zM9.9 9.4c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5c.1 1.4-1.1 2.5-2.5 2.5zm0-3.3c-.4 0-.8.3-.8.8 0 .4.3.8.8.8.5-.1.8-.4.8-.8 0-.5-.3-.8-.8-.8z"/><path d="M10 13.7h-.2c-1-.1-1.8-.8-1.8-1.8v-.1h-.1l-.1.1c-.8.7-2.1.6-2.8-.2s-.7-1.9 0-2.6l.1-.1H5c-1.1 0-2-.8-2.1-1.9 0-1.2.8-2.1 1.8-2.2H5v-.1c-.7-.8-.7-2 .1-2.8.8-.7 1.9-.7 2.7 0 .1 0 .1 0 .2-.1 0-.6.3-1.1.7-1.4.8-.7 2.1-.6 2.8.2.2.3.4.7.4 1.1v.1h.1c.8-.7 2.1-.6 2.8.2.6.7.6 1.9 0 2.6l-.1.1v.1h.1c.5 0 1 .1 1.4.5.8.7.9 2 .2 2.8-.3.4-.8.6-1.4.7h-.3c.4.4.6 1 .6 1.5-.1 1.1-1 1.9-2.1 1.9-.4 0-.9-.2-1.2-.5l-.1-.1v.1c0 1.1-.9 1.9-1.9 1.9zM7.9 10c1 0 1.8.8 1.8 1.7 0 .1.1.2.2.2s.2-.1.2-.2c0-1 .8-1.8 1.8-1.8.5 0 .9.2 1.3.5.1.1.2.1.3 0s.1-.2 0-.3c-.7-.7-.7-1.8 0-2.5.3-.3.8-.5 1.3-.5h.1c.1 0 .2 0 .2-.1 0 0 .1-.1.1-.2s0-.1-.1-.2c0 0-.1-.1-.2-.1h-.2c-.7 0-1.4-.4-1.6-1.1 0-.1 0-.1-.1-.2-.2-.6-.1-1.3.4-1.8.1-.1.1-.2 0-.3s-.2-.1-.3 0c-.3.3-.8.5-1.2.5-1 0-1.8-.8-1.8-1.8 0-.1-.1-.2-.2-.2s-.1 0-.2.1c.1.1 0 .2 0 .3 0 .7-.4 1.4-1.1 1.7-.1 0-.1 0-.2.1-.6.2-1.3 0-1.8-.4-.1-.1-.2-.1-.3 0-.1.1-.1.2 0 .3.3.3.5.7.5 1.2.1 1-.7 1.9-1.7 1.9h-.2c-.1 0-.1 0-.2.1 0-.1 0 0 0 0 0 .1.1.2.2.2h.2c1 0 1.8.8 1.8 1.8 0 .5-.2.9-.5 1.2-.1.1-.1.2 0 .3s.2.1.3 0c.3-.2.7-.4 1.1-.4h.1z"/></g></svg>
                    <template v-if="$route.params.hasOwnProperty('name')">
						<router-link :to="'/' + $route.params.namespace + '/sgdbops'" title="SGDbOps">SGDbOps</router-link>
					</template>
					<template v-else>
                        SGDbOps
                    </template>
                </li>
                <li v-if="(typeof $route.params.name !== 'undefined')">
                    {{ $route.params.name }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgdbops/" target="_blank" title="SGDbOps Documentation">SGDbOps Documentation</a>
                <div>
                    <template v-if="$route.params.hasOwnProperty('name')">
                        <a v-if="iCan('delete','sgdbops',$route.params.namespace)" title="Delete Operation" @click="deleteCRD('sgdbops',$route.params.namespace, $route.params.name, '/' + $route.params.namespace + '/sgdbops')">
                            Delete Operation
                        </a>
                        <router-link class="borderLeft" :to="'/' + $route.params.namespace + '/sgdbops'" title="Close Details">Close Details</router-link>
                    </template>
                    <template v-else>
                        <router-link v-if="iCan('create','sgdbops',$route.params.namespace)"  :to="'/' + $route.params.namespace + '/sgdbops/new'" class="add">Add New</router-link>
                    </template>
                </div>	
            </div>		
        </header>

        <div class="content">
            <template v-if="!$route.params.hasOwnProperty('name')">
                <div class="toolbar">
                    <div class="searchBar">
                        <input id="keyword" v-model="filters.keyword" class="search" placeholder="Search by name..." autocomplete="off">
                        <a @click="filterOps('keyword')" class="btn" v-if="filters.keyword.length">APPLY</a>
                        <a @click="clearFilters('keyword')" class="btn clear border keyword" v-if="filters.keyword.length">CLEAR</a>
                    </div>


                    <div class="filter filters">
                        <span class="toggle">FILTER</span>

                        <ul class="options">
                            <li>
                                <span>Operation Type</span>
                                <select v-model="filters.op">
                                    <option value="">All Types</option>
                                    <option value="benchmark">Benchmark</option>
                                    <option value="vacuum">Vacuum</option>
                                    <option value="repack">Repack</option>
                                    <option value="securityUpgrade">Security Upgrade</option>
                                    <option value="minorVersionUpgrade">Minor Version Upgrade</option>
                                    <option value="majorVersionUpgrade">Major Version Upgrade</option>
                                    <option value="restart">Restart</option>
                                </select>
                            </li>
                            <li>
                                <span>Status</span>
                                <label for="isCompleted">
                                    <input v-model="filters.status" data-filter="status" type="checkbox" id="isCompleted" name="isCompleted" value="Completed" :class="( filters.status.includes('Completed') ? 'active' : '' )"/>
                                    <span>Completed</span>
                                </label>
                                <label for="isRunning">
                                    <input v-model="filters.status" data-filter="status" type="checkbox" id="isRunning" name="isRunning" value="Running" :class="( filters.status.includes('Running') ? 'active' : '' )"/>
                                    <span>Running</span>
                                </label>
                                <label for="isFailed">
                                    <input v-model="filters.status" data-filter="status" type="checkbox" id="isFailed" name="isFailed" value="Failed" :class="( filters.status.includes('Failed') ? 'active' : '' )"/>
                                    <span>Failed</span>
                                </label>	
                            </li>
                            <li>
                                <span>Target Cluster</span>
                                <select v-model="filters.clusterName">
                                    <option value="">All Clusters</option>
                                    <template v-for="cluster in clusters">
                                        <option v-if="cluster.data.metadata.namespace == $route.params.namespace">{{ cluster.data.metadata.name }}</option>
                                    </template>
                                </select>
                            </li>
                            <li>
                                <hr>
                                <a class="btn" @click="filterOps('others')">APPLY</a> <a class="btn clear border" @click="clearFilters('others')" v-if="isFiltered">CLEAR</a>
                            </li>
                        </ul>
                    </div> 
                </div>
                
                <table id="sgdbops" class="dbops resizable" v-columns-resizable>
                    <thead class="sort">
                        <th class="asc start hasTooltip">
                            <span @click="sort('data.spec.runAt', 'timestamp')" title="Start">Start</span>
                            <span class="helpTooltip" :data-tooltip="(timezone == 'local') ? getTooltip('sgdbops.spec.runAt').replace('UTC ','') : getTooltip('sgdbops.spec.runAt')"></span>
                        </th>
                        <th class="asc operationType">
                            <span @click="sort('data.spec.op')" title="Operation">Operation</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.op')"></span>
                        </th>
                        <th class="asc opName hasTooltip">
                            <span @click="sort('data.metadata.name')" title="Name">Name</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.name')"></span>
                        </th>
                        <th class="asc phase hasTooltip">
                            <span @click="sort('data.status.conditions')" title="Status">Status</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions')"></span>
                        </th>
                        <th class="asc targetCluster hasTooltip">
                            <span @click="sort('data.spec.sgCluster')" title="Target Cluster">Target Cluster</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.sgCluster')"></span>
                        </th>
                        <th class="asc elapsed textRight hasTooltip">
                            <span @click="sort('data.status.elapsed', 'duration')" title="Elapsed">Elapsed</span>
                            <span class="helpTooltip" data-tooltip="The time between the moment the operation started and the moment on which it reached its current status."></span>
                        </th>
                        <th class="asc retries textRight hasTooltip">
                            <span @click="sort('data.status.opRetries')" title="Current / Max Retries">Current / Max Retries</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.opRetries') + ' / '+ getTooltip('sgdbops.spec.maxRetries')"></span> 
                        </th>
                        <th class="asc timedOut textRight hasTooltip">
                            <span @click="sort('data.spec.timeout')" title="Timed Out">Timed Out</span>
                            <span class="helpTooltip" data-tooltip="States whether the operation failed because of timeout expiration."></span>
                        </th>
                        <th class="actions"></th>
                    </thead>

                    <tbody>
                        <template v-if="!dbOps.length">
							<tr class="no-results">
								<td colspan="8" v-if="iCan('create','sgdbops',$route.params.namespace)">
									No database operations have been found, would you like to <router-link :to="'/' + $route.params.namespace + '/sgdbops/new'" title="Add New Database Operation">create a new one?</router-link>
								</td>
								<td v-else colspan="8">
									No database operations have been found. You don't have enough permissions to create a new one
								</td>
							</tr>
						</template>
                        <template v-for="(op, index) in dbOps">
                            <template  v-if="(index >= pagination.start) && (index < pagination.end)">
                                <tr class="base">
                                    <td class="timestamp hasTooltip">
                                        <span>
                                            <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                                <template v-if="op.data.spec.hasOwnProperty('runAt')">
                                                    <span class='date'>
                                                        {{ op.data.spec.runAt | formatTimestamp('date') }}
                                                    </span>
                                                    <span class='time'>
                                                        {{ op.data.spec.runAt | formatTimestamp('time') }}
                                                    </span>
                                                    <span class='tzOffset'>{{ showTzOffset() }}</span>
                                                </template>
                                                <template v-else-if="hasProp(op,'data.status.opStarted')">
                                                    <span class='date'>
                                                        {{ op.data.status.opStarted | formatTimestamp('date') }}
                                                    </span>
                                                    <span class='time'>
                                                        {{ op.data.status.opStarted | formatTimestamp('time') }}
                                                    </span>
                                                    <span class='tzOffset'>{{ showTzOffset() }}</span>
                                                </template>
                                                <span v-else class="asap">
                                                    ASAP
                                                </span>
                                            </router-link>
                                        </span>
                                    </td>
                                    <td :class="op.data.spec.op" class="operationType">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            <span>
                                                {{ op.data.spec.op }}
                                            </span>
                                        </router-link>                                        
                                    </td>
                                    <td class="baseHide opName hasTooltip">
                                        <span>
                                            <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                                {{ op.data.metadata.name }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="phase center baseHide">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            <span :class="getOpStatus(op)">                                            
                                                {{ getOpStatus(op) }}
                                            </span>
                                        </router-link>
                                    </td>
                                    <td class="baseHide targetCluster hasTooltip">
                                        <span>
                                            <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                                {{ op.data.spec.sgCluster }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="timestamp baseHide elapsed textRight hasTooltip">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            <span class="time" v-if="op.data.hasOwnProperty('status')" v-html="getElapsedTime(op)"></span>
                                        </router-link>
                                    </td>
                                    <td class="baseHide retries textRight">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            <span>
                                                {{ (op.data.hasOwnProperty('status') && op.data.status.hasOwnProperty('opRetries')) ? op.data.status.opRetries : '0' }}
                                            </span>
                                            / 
                                            <span>
                                                {{ op.data.spec.hasOwnProperty('maxRetries') ? op.data.spec.maxRetries : '1' }}
                                            </span>
                                        </router-link>
                                    </td>
                                    <td class="baseHide timedOut textRight">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            {{ hasTimedOut(op) }}
                                        </router-link>
                                    </td>
                                    <td class="actions textRight">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" target="_blank" class="newTab">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="15.001" height="12.751" viewBox="0 0 15.001 12.751"><g transform="translate(167.001 -31.5) rotate(90)"><path d="M37.875,168.688a.752.752,0,0,1-.53-.219l-5.625-5.626a.75.75,0,0,1,0-1.061l2.813-2.813a.75.75,0,0,1,1.06,1.061l-2.283,2.282,4.566,4.566,4.566-4.566-2.283-2.282a.75.75,0,0,1,1.06-1.061l2.813,2.813a.75.75,0,0,1,0,1.061l-5.625,5.626A.752.752,0,0,1,37.875,168.688Z" transform="translate(0 -1.687)" fill="#00adb5"/><path d="M42.156,155.033l-2.813-2.813a.752.752,0,0,0-1.061,0l-2.813,2.813a.75.75,0,1,0,1.06,1.061l1.533-1.534v5.3a.75.75,0,1,0,1.5,0v-5.3l1.533,1.534a.75.75,0,1,0,1.06-1.061Z" transform="translate(-0.937 0)" fill="#00adb5"/></g></svg>
                                        </router-link>
                                        <a class="delete" title="Delete Operation" @click="deleteCRD('sgdbops',$route.params.namespace, op.name)">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
                                        </a>
                                    </td>
                                </tr>
                            </template>
                        </template>
                    </tbody>
                </table>
                <v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < dbOps.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="dbOps.length" @page-change="pageChange" align="center" ref="page"></v-page>
                <div id="nameTooltip">
                    <div class="info"></div>
                </div>
            </template>
            <template v-else>
                <h2>Operation Details</h2>
                <template v-for="(op, index) in dbOps" v-if="op.name == $route.params.name">
                    
                    <table class="crdDetails">
                        <tbody>
                            <tr>
                                <td class="label">
                                    Name
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.name')"></span>
                                </td>
                                <td colspan="2">
                                    {{ op.data.metadata.name }}
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    UID
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.uid')"></span>
                                </td>
                                <td colspan="2">
                                    {{ op.data.metadata.uid }}
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Target Cluster
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.sgCluster')"></span>
                                </td>
                                <td colspan="2">
                                    {{ op.data.spec.sgCluster }}
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Max Retries
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.maxRetries')"></span>
                                </td>
                                <td colspan="2">
                                    {{ op.data.spec.hasOwnProperty('maxRetries') ? op.data.spec.maxRetries : '1' }}
                                </td>
                            </tr>
                            <tr v-if="op.data.spec.hasOwnProperty('timeout')">
                                <td class="label">
                                    Timeout
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.timeout')"></span>
                                </td>
                                <td colspan="2">
                                        {{ getIsoDuration(op.data.spec.timeout) }}
                                </td>
                            </tr>
                            <tr v-if="op.data.spec.hasOwnProperty('timeout')">
                                <td class="label">
                                    Timed Out
                                    <span class="helpTooltip" data-tooltip="States whether the operation failed because of timeout expiration."></span>
                                </td>
                                <td colspan="2">
                                    {{ hasTimedOut(op) }}
                                </td>
                            </tr>
                        </tbody>
                    </table>

                    <h2 class="capitalize">
                        {{ op.data.spec.op }} Specs
                    </h2>
                    <table class="crdDetails">
                        <template v-if="op.data.spec.op === 'benchmark' && op.data.spec.hasOwnProperty('benchmark')">
                            <tbody>
                                <tr>
                                    <td class="label">
                                        Type
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.type')"></span>
                                    </td>
                                    <td colspan="2">
                                        {{ op.data.spec.benchmark.type }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label" :rowspan="Object.keys(op.data.spec.benchmark.pgbench).length + 1">
                                        PgBench
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench')"></span>
                                    </td>
                                    <td class="hidden"></td><td class="hidden"></td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Database Size
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.databaseSize')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.benchmark.pgbench.databaseSize }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Duration
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.duration')"></span>
                                    </td>
                                    <td>
                                        {{ getIsoDuration(op.data.spec.benchmark.pgbench.duration) }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Use Prepared Statements
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.usePreparedStatements')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.benchmark.pgbench.hasOwnProperty('usePreparedStatements') ? op.data.spec.benchmark.pgbench.usePreparedStatements : 'False' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Concurrent Clients
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.concurrentClients')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.benchmark.pgbench.hasOwnProperty('concurrentClients') ? op.data.spec.benchmark.pgbench.concurrentClients : '1' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Threads
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.threads')"></span>
                                    </td>
                                    <td colspan="2">
                                        {{ op.data.spec.benchmark.pgbench.hasOwnProperty('threads') ? op.data.spec.benchmark.pgbench.threads : '1' }}
                                    </td>
                                </tr>
                                <tr v-if="op.data.spec.benchmark.hasOwnProperty('connectionType')">
                                    <td class="label">
                                        Connection Type
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.connectionType')"></span>
                                    </td>
                                    <td colspan="2">
                                        {{ op.data.spec.benchmark.connectionType }}
                                    </td>
                                </tr>
                            </tbody>               
                        </template>

                        <template v-else-if="op.data.spec.op === 'majorVersionUpgrade' && op.data.spec.hasOwnProperty('majorVersionUpgrade')">
                            <tbody>
                                <tr>
                                    <td class="label">
                                        Link
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.link')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.majorVersionUpgrade.hasOwnProperty('link') ? op.data.spec.majorVersionUpgrade.link : 'False' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Clone
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.clone')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.majorVersionUpgrade.hasOwnProperty('clone') ? op.data.spec.majorVersionUpgrade.clone : 'False' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Check
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.check')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.majorVersionUpgrade.hasOwnProperty('check') ? op.data.spec.majorVersionUpgrade.check : 'False' }}
                                    </td>
                                </tr>
                            </tbody>
                        </template>

                        <template v-else-if="op.data.spec.op === 'minorVersionUpgrade' && op.data.spec.hasOwnProperty('minorVersionUpgrade')">
                            <tbody>
                                <tr v-if="op.data.spec.minorVersionUpgrade.hasOwnProperty('method')">
                                    <td class="label">
                                        Method
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.method')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.minorVersionUpgrade.method }}
                                    </td>
                                </tr>
                            </tbody>
                        </template>

                        <template v-else-if="op.data.spec.op === 'repack' && op.data.spec.hasOwnProperty('repack')">
                            <tbody>
                                    <tr>
                                        <td class="label">
                                            No Order
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noOrder')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.spec.repack.hasOwnProperty('noOrder') ? op.data.spec.repack.noOrder : 'False' }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.spec.repack.hasOwnProperty('waitTimeout')">
                                        <td class="label">
                                            Wait Timeout
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.waitTimeout')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.spec.repack.waitTimeout }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Kill Backend
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noKillBackend')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.repack.hasOwnProperty('noKillBackend') ? op.data.spec.repack.noKillBackend : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.repack.hasOwnProperty('noAnalyze') ? op.data.spec.repack.noAnalyze : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Exclude Extension
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.repack.hasOwnProperty('excludeExtension') ? op.data.spec.repack.excludeExtension : 'False' }}
                                        </td>
                                    </tr>
                                </tbody>
                        </template>

                        <template v-else-if="op.data.spec.op === 'restart' && op.data.spec.hasOwnProperty('restart')">
                            <tbody>
                                <tr v-if="op.data.spec.restart.hasOwnProperty('method')">
                                    <td class="label">
                                        Method
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart.method')"></span> 
                                    </td>
                                    <td>
                                        {{ op.data.spec.restart.method }}
                                    </td>
                                </tr>
                            </tbody>
                        </template>

                        <template v-else-if="op.data.spec.op === 'securityUpgrade' && op.data.spec.hasOwnProperty('securityUpgrade')">
                            <tbody>
                                <tr v-if="op.data.spec.securityUpgrade.hasOwnProperty('method')">
                                    <td class="label">
                                        Method
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.securityUpgrade.method')"></span> 
                                    </td>
                                    <td>
                                        {{ op.data.spec.securityUpgrade.method }}
                                    </td>
                                </tr>
                            </tbody>
                        </template>

                        <template v-else-if="op.data.spec.op === 'vacuum' && op.data.spec.hasOwnProperty('vacuum')">
                            <tbody>
                                    <tr>
                                        <td class="label">
                                            Full
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.full')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.vacuum.hasOwnProperty('full') ? op.data.spec.vacuum.full : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Freeze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.vacuum.hasOwnProperty('freeze') ? op.data.spec.vacuum.freeze : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.vacuum.hasOwnProperty('analyze') ? op.data.spec.vacuum.analyze : 'True' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Disable PageSkipping
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping')"></span> 
                                        </td>
                                        <td>
                                            {{ op.data.spec.vacuum.hasOwnProperty('disablePageSkipping') ? op.data.spec.vacuum.disablePageSkipping : 'False' }}
                                        </td>
                                    </tr>
                                </tbody>
                        </template>
                    </table> 

                    <template v-if="op.data.spec.op === 'repack' && op.data.spec.hasOwnProperty('repack') && op.data.spec.repack.hasOwnProperty('databases') && op.data.spec.repack.databases.length">
                        <h2 class="capitalize">
                            {{ op.data.spec.op }} Databases
                        </h2>
                        <table>
                            <tbody>
                                <template v-for="db in op.data.spec.repack.databases">
                                    <tr>
                                        <td class="label" :rowspan="Object.keys(db).length">
                                            {{ db.name }}
                                        </td>
                                        <td class="hidden"></td><td class="hidden"></td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Order
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noOrder')"></span>
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('noOrder') ? db.noOrder : 'False' }}
                                        </td>
                                    </tr>
                                    <tr v-if="db.hasOwnProperty('waitTimeout')">
                                        <td class="label">
                                            Wait Timeout
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.waitTimeout')"></span>
                                        </td>
                                        <td>
                                            {{ db.waitTimeout }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Kill Backend
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noKillBackend')"></span>
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('noKillBackend') ? db.noKillBackend : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze')"></span>
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('noAnalyze') ? db.noAnalyze : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Exclude Extension
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span>
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('excludeExtension') ? db.excludeExtension : 'False' }}
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>
                    </template>

                    <template v-else-if="op.data.spec.op === 'vacuum' && op.data.spec.hasOwnProperty('vacuum') && op.data.spec.vacuum.hasOwnProperty('databases') && op.data.spec.vacuum.databases.length">
                        <h2 class="capitalize">
                            {{ op.data.spec.op }} Databases
                        </h2>
                        <table>
                            <tbody>
                                <template v-for="db in op.data.spec.vacuum.databases">
                                    <tr>
                                        <td class="label" :rowspan="Object.keys(db).length">
                                            {{ db.name }}
                                        </td>
                                        <td class="hidden"></td><td class="hidden"></td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Full
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.full')"></span> 
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('full') ? db.full : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Freeze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span> 
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('freeze') ? db.freeze : 'False' }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span> 
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('analyze') ? db.analyze : 'True' }}
                                        </td>
                                    </tr>
                                    <tr v-if="db.hasOwnProperty('disablePageSkipping')">
                                        <td class="label">
                                            Disable PageSkipping
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping')"></span> 
                                        </td>
                                        <td>
                                            {{ db.hasOwnProperty('disablePageSkipping') ? db.disablePageSkipping : 'False' }}
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>
                    </template>

                    <template v-if="op.data.hasOwnProperty('status')">
                        <h2 class="capitalize">
                            Conditions
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions')"></span> 
                        </h2>
                        <table class="opConditions crdDetails">
                            <tbody>
                                <template v-for="condition in op.data.status.conditions">
                                    <tr>
                                        <td class="label" :rowspan="Object.keys(condition).length">
                                            {{ condition.type }}
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.type')"></span>
                                        </td>
                                        <td class="hidden"></td><td class="hidden"></td>
                                    </tr>
                                    <tr v-if="condition.hasOwnProperty('lastTransitionTime')">
                                        <td class="label">
                                            Last Transition Time
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.lastTransitionTime')"></span>
                                        </td>
                                        <td class="timestamp">
                                            <span class='date'>
                                                {{ condition.lastTransitionTime | formatTimestamp('date') }}
                                            </span>
                                            <span class='time'>
                                                {{ condition.lastTransitionTime | formatTimestamp('time') }}
                                            </span>
                                            <span class='tzOffset'>{{ showTzOffset() }}</span>
                                        </td>
                                    </tr>
                                    <tr v-if="condition.hasOwnProperty('reason')">
                                        <td class="label">
                                            Reason
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.reason')"></span>
                                        </td>
                                        <td>
                                            {{ condition.reason }}
                                        </td>
                                    </tr>
                                    <tr v-if="condition.hasOwnProperty('status')">
                                        <td class="label">
                                            Status
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.status')"></span>
                                        </td>
                                        <td>
                                            {{ condition.status }}
                                        </td>
                                    </tr>
                                    <tr v-if="condition.hasOwnProperty('message')">
                                        <td class="label">
                                            Message
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.message')"></span>
                                        </td>
                                        <td>
                                            {{ condition.message }}
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>

                        <h2>Operation Status</h2>
                        <table class="crdDetails">
                            <tbody>
                                <tr v-if="op.data.status.hasOwnProperty('opStarted')">
                                    <td class="label">
                                        Operation Started
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.opStarted')"></span>
                                    </td>
                                    <td class="timestamp" colspan="2">
                                        <span class='date'>
                                            {{ op.data.status.opStarted | formatTimestamp('date') }}
                                        </span>
                                        <span class='time'>
                                            {{ op.data.status.opStarted | formatTimestamp('time') }}
                                        </span>
                                        <span class='tzOffset'>{{ showTzOffset() }}</span>
                                    </td>
                                </tr>
                                <tr v-if="op.data.status.hasOwnProperty('opRetries')">
                                    <td class="label">
                                        Operation Retries
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.opRetries')"></span>
                                    </td>
                                    <td colspan="2">
                                        {{ op.data.status.opRetries }}
                                    </td>
                                </tr>

                                <template v-if="op.data.spec.op === 'benchmark' && op.data.status.hasOwnProperty('benchmark')">
                                    <tr v-if="op.data.status.benchmark.hasOwnProperty('pgbench')">
                                        <td class="label" :rowspan="Object.keys(op.data.status.benchmark.pgbench).length + 1">
                                            PgBench
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench')"></span>
                                        </td>
                                        <td class="hidden"></td><td class="hidden"></td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('scaleFactor')">
                                        <td class="label">
                                            Scale Factor
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.scaleFactor')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status.benchmark.pgbench.scaleFactor }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('transactionsProcessed')">
                                        <td class="label">
                                            Transactions Processed
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsProcessed')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status.benchmark.pgbench.transactionsProcessed }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('latencyAverage')">
                                        <td class="label">
                                            Latency Average
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latencyAverage')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status.benchmark.pgbench.latencyAverage }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('latencyStddev')">
                                        <td class="label">
                                            Latency Stddev
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latencyStddev')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status.benchmark.pgbench.latencyStddev }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('tpsIncludingConnectionsEstablishing')">
                                        <td class="label">
                                            Tps Including Connections Establishing
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.tpsIncludingConnectionsEstablishing')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status.benchmark.pgbench.tpsIncludingConnectionsEstablishing }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('tpsExcludingConnectionsEstablishing')">
                                        <td class="label">
                                            Tps Excluding Connections Establishing
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.tpsExcludingConnectionsEstablishing')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status.benchmark.pgbench.tpsExcludingConnectionsEstablishing }}
                                        </td>
                                    </tr>
                                </template>

                                <template v-else-if="op.data.status.hasOwnProperty(op.data.spec.op)">
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('primaryInstance')">
                                        <td class="label">
                                            Primary Instance
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.primaryInstance')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status[op.data.spec.op].primaryInstance }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('initialInstances') && op.data.status[op.data.spec.op].initialInstances.length">
                                        <td class="label" :rowspan="Object.keys(op.data.status[op.data.spec.op].initialInstances).length + 1">
                                            Initial Instances
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.initialInstances')"></span>
                                        </td>
                                        <td class="hidden"></td>
                                    </tr>
                                    <template v-for="instance in op.data.status[op.data.spec.op].initialInstances">
                                        <tr>
                                            <td>
                                                {{ instance }}
                                            </td>
                                        </tr>
                                    </template>
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('pendingToRestartInstances') && op.data.status[op.data.spec.op].pendingToRestartInstances.length">
                                        <td class="label" :rowspan="Object.keys(op.data.status[op.data.spec.op].pendingToRestartInstances).length + 1">
                                            Pending To Restart Instances
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.pendingToRestartInstances')"></span>
                                        </td>
                                        <td class="hidden"></td>
                                    </tr>
                                    <template v-for="instance in op.data.status[op.data.spec.op].pendingToRestartInstances">
                                        <tr>
                                            <td>
                                                {{ instance }}
                                            </td>
                                        </tr>
                                    </template>
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('restartedInstances') && op.data.status[op.data.spec.op].restartedInstances.length">
                                        <td class="label" :rowspan="Object.keys(op.data.status[op.data.spec.op].restartedInstances).length + 1">
                                            Restarted Instances
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.restartedInstances')"></span>
                                        </td>
                                        <td class="hidden"></td>
                                    </tr>
                                    <template v-for="instance in op.data.status[op.data.spec.op].restartedInstances">
                                        <tr>
                                            <td>
                                                {{ instance }}
                                            </td>
                                        </tr>
                                    </template>
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('switchoverInitiated')">
                                        <td class="label">
                                            Switchover Initiated
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.switchoverInitiated')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status[op.data.spec.op].switchoverInitiated }}
                                            
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('failure')">
                                        <td class="label">
                                            Failure
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.failure')"></span>
                                        </td>
                                        <td>
                                            {{ op.data.status[op.data.spec.op].failure }}
                                        </td>
                                    </tr>
                                </template>
                            </tbody>
                        </table>
                    </template>
                </template>
            </template>
        </div>
    </div>
</template>

<script>
    import { mixin } from './mixins/mixin'
    import store from '../store'
    import moment from 'moment'

    export default {
        name: 'DbOps',

        mixins: [mixin],

        data: function() {
            return {
                currentSort: {
                    param: 'data.spec.runAt',
                    type: 'timestamp'
                },
				currentSortDir: 'asc',
                filters: {
					clusterName: '',
					keyword: '',
					op: '',
					status: [],
				},
				activeFilters: {
					clusterName: '',
					keyword: '',
					op: '',
					status: [],
				}
            }
        },
        methods: {

            getOpStatus(op) {
                const vc = this
                let status = ''

                if (vc.hasProp(op, 'data.status.conditions')) {
                    op.data.status.conditions.forEach(function(cond) {
                        if(cond.status == 'True') {
                            status = cond.type
                            return false
                        }
                        
                    })
                }

                return status
            },

            getIsoDuration(duration) {
                let d = (duration.split('P').pop().split('D')[0] != '0') ? duration.split('P').pop().split('D')[0] : ''
                let h = (duration.split('T').pop().split('H')[0] != '0') ? duration.split('T').pop().split('H')[0] : ''
                let m = (duration.split('H').pop().split('M')[0] != '0') ? duration.split('H').pop().split('M')[0] : ''
                let s = (duration.split('M').pop().split('S')[0] != '0') ? duration.split('M').pop().split('S')[0] : ''

                return (
                    (d.length ? (d + ' day' + ( (d != '1') ? 's' : '' ) ) : '') +
                    (h.length ? ( (d.length ? ', ' : '') + (h + ' hour' + ( (h != '1') ? 's' : '' ) ) ) : '') +
                    (m.length ? ( ( (d.length || h.length) ? ', ' : '') + (m + ' minute' + ( (m != '1') ? 's' : '' ) ) ) : '') +
                    (s.length ? ( ( (d.length || h.length || m.length) ? ', ' : '') + (s + ' second' + ( (s != '1') ? 's' : '' ) ) ) : '')
                )
            },

            getElapsedTime(op) {

                if( op.data.hasOwnProperty('status') ) {
                    let lastStatus = op.data.status.conditions.find(c => (c.status === 'True') )
                    let begin = moment(op.data.status.opStarted)
                    let finish = (lastStatus.type == 'Running') ? moment() : moment(lastStatus.lastTransitionTime);
                    let elapsed = moment.duration(finish.diff(begin));
                    return elapsed.toString().substring(2).replace('H','h ').replace('M','m ').replace(/\..*S/,'s')
                } else {
                    return '-'
                }   
            },

            hasTimedOut(op) {
                if( op.data.hasOwnProperty('status') ) {
                    let failedOp = op.data.status.conditions.find(c => (c.status === 'True') && (c.type == 'Failed') )

                    if( (typeof failedOp !== 'undefined') && (failedOp.reason == 'DbOpsTimedOut') )
                        return 'YES (' + op.data.spec.timeout + ')'
                    else
                        return 'NO'

                } else {
                    return 'NO'
                }
            },

            clearFilters(section) {
				const vc = this
				
				if(section == 'others') {
					vc.filters.clusterName = '';
					vc.activeFilters.clusterName = '';
					
					vc.filters.op = '';
					vc.activeFilters.op = '';
					
					vc.filters.status = [];
					vc.activeFilters.status = [];

					$('.filters.open .active').removeClass('active');

				} else if (section == 'keyword') {
					vc.filters.keyword = '';
					vc.activeFilters.keyword = '';
				}

			},

			filterOps(section) {
				const vc = this

				switch(section) {

					case 'keyword':
						vc.activeFilters.keyword = vc.filters.keyword
						break;
					
					case 'others':
						Object.keys(vc.filters).forEach(function(filter) {
							if(!filter.includes('keyword'))
								vc.activeFilters[filter] = vc.filters[filter]
						})
						break;

				}
				
			},

        },
        
        computed: {

            dbOps () {
                const vc = this
				
				store.state.dbOps.forEach( function(op, index) {

					let show = true

					if(vc.activeFilters.keyword.length)
						show = JSON.stringify(op).includes(vc.activeFilters.keyword)
						
					if(vc.activeFilters.status.length && show)
						show = (typeof ( op.data.status.conditions.find(c => ( (c.status == "True") && vc.activeFilters.status.includes(c.type) ) ) ) !== 'undefined')                    

					if(vc.activeFilters.clusterName.length && show)
						show = (vc.activeFilters.clusterName == op.data.spec.sgCluster)

                    if(vc.activeFilters.op.length && show)
						show = (vc.activeFilters.op == op.data.spec.op)
					
					if(op.show != show) {
						store.commit('showDbOp',{
							pos: index,
							isVisible: show
						})
					}

				})

				return vc.sortTable( [...(store.state.dbOps.filter(op => ( op.show && ( op.data.metadata.namespace == vc.$route.params.namespace ))))], vc.currentSort.param, vc.currentSortDir, vc.currentSort.type)
            },
            
            tooltips() {
                return store.state.tooltips
            },

            clusters() {
                return store.state.clusters
            },

            isFiltered() {
                return ( this.filters.clusterName.length || this.filters.op.length || this.filters.status.length)
            },

            timezone () {
                return store.state.timezone
            }
        }
    }
</script>

<style scoped>
    .opSpec th, .opStatus th {
        text-transform: capitalize;
    }

    tr.details table {
        margin-bottom: 12px;
    }

    td.elapsed, td.retries, td.timedOut {
        padding-right: 5px;
    }

    td.phase span.Completed {
        color: #03CC03;
        background: #00F90040;
    }

    td.phase span.Running {
        color: #DE9826;
        background: #FCC12040;
    }

    td.phase span.Failed {
        color: #FF2600;
        background: #FF260040;
    }

    tr.base.open .baseHide {
        opacity: 0;
    }

    th.actions, td.actions {
        width: 75px !important;
        min-width: 75px;
        max-width: 75px;
    }

    table#sgdbops, #sgdbops thead, #sgdbops th, #sgdbops tbody, #sgdbops tr, #sgdbops td {
        background: none;
    }

    #sgdbops tr:nth-child(even), #sgdbops tr.details:nth-child(odd) {
        background-color: var(--activeBg);
    }

    #sgdbops tr:nth-child(odd), #sgdbops tr.details:nth-child(even) {
        background-color: var(--rowBg);
    }

</style>