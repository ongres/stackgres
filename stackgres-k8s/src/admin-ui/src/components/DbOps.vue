<template>
    <div id="dbops" v-if="loggedIn && isReady">
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20.026" height="27"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                    <router-link :to="'/overview/'+$route.params.namespace" title="Namespace Overview">{{ $route.params.namespace }}</router-link>
                </li>
                <li>
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M17.1 20c-.6 0-1-.5-1-1 0-1.6-1.3-2.8-2.8-2.8H6.6c-1.6 0-2.8 1.3-2.8 2.8 0 .6-.5 1-1 1s-1-.5-1-1c0-2.7 2.2-4.8 4.8-4.8h6.7c2.7 0 4.8 2.2 4.8 4.8.1.5-.4 1-1 1zM9.9 9.4c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5c.1 1.4-1.1 2.5-2.5 2.5zm0-3.3c-.4 0-.8.3-.8.8 0 .4.3.8.8.8.5-.1.8-.4.8-.8 0-.5-.3-.8-.8-.8z"/><path d="M10 13.7h-.2c-1-.1-1.8-.8-1.8-1.8v-.1h-.1l-.1.1c-.8.7-2.1.6-2.8-.2s-.7-1.9 0-2.6l.1-.1H5c-1.1 0-2-.8-2.1-1.9 0-1.2.8-2.1 1.8-2.2H5v-.1c-.7-.8-.7-2 .1-2.8.8-.7 1.9-.7 2.7 0 .1 0 .1 0 .2-.1 0-.6.3-1.1.7-1.4.8-.7 2.1-.6 2.8.2.2.3.4.7.4 1.1v.1h.1c.8-.7 2.1-.6 2.8.2.6.7.6 1.9 0 2.6l-.1.1v.1h.1c.5 0 1 .1 1.4.5.8.7.9 2 .2 2.8-.3.4-.8.6-1.4.7h-.3c.4.4.6 1 .6 1.5-.1 1.1-1 1.9-2.1 1.9-.4 0-.9-.2-1.2-.5l-.1-.1v.1c0 1.1-.9 1.9-1.9 1.9zM7.9 10c1 0 1.8.8 1.8 1.7 0 .1.1.2.2.2s.2-.1.2-.2c0-1 .8-1.8 1.8-1.8.5 0 .9.2 1.3.5.1.1.2.1.3 0s.1-.2 0-.3c-.7-.7-.7-1.8 0-2.5.3-.3.8-.5 1.3-.5h.1c.1 0 .2 0 .2-.1 0 0 .1-.1.1-.2s0-.1-.1-.2c0 0-.1-.1-.2-.1h-.2c-.7 0-1.4-.4-1.6-1.1 0-.1 0-.1-.1-.2-.2-.6-.1-1.3.4-1.8.1-.1.1-.2 0-.3s-.2-.1-.3 0c-.3.3-.8.5-1.2.5-1 0-1.8-.8-1.8-1.8 0-.1-.1-.2-.2-.2s-.1 0-.2.1c.1.1 0 .2 0 .3 0 .7-.4 1.4-1.1 1.7-.1 0-.1 0-.2.1-.6.2-1.3 0-1.8-.4-.1-.1-.2-.1-.3 0-.1.1-.1.2 0 .3.3.3.5.7.5 1.2.1 1-.7 1.9-1.7 1.9h-.2c-.1 0-.1 0-.2.1 0-.1 0 0 0 0 0 .1.1.2.2.2h.2c1 0 1.8.8 1.8 1.8 0 .5-.2.9-.5 1.2-.1.1-.1.2 0 .3s.2.1.3 0c.3-.2.7-.4 1.1-.4h.1z"/></g></svg>
                    SGDbOps
                </li>
                <li v-if="(typeof $route.params.name !== 'undefined')">
                    {{ $route.params.name }}
                </li>
            </ul>

            <div class="actions">
                <a class="documentation" href="https://stackgres.io/doc/latest/reference/crd/sgdbops/" target="_blank" title="SGDbOps Documentation">SGDbOps Documentation</a>
                <div>
                    <router-link :to="'/crd/create/dbops/'+$route.params.namespace" class="add">Add New</router-link>
                </div>	
            </div>		
        </header>

        <div class="content">
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
                    <tr class="no-results">
                        <td colspan="999">
                            No records matched your search terms
                        </td>
                    </tr>

                    <template v-for="(op, index) in dbOps">
                        <tr class="base" @click="goTo('/dbops/' + $route.params.namespace + '/' + op.data.metadata.name)" :class="[($route.params.name == op.data.metadata.name) ? 'open': '', ( (index < pagination.start) || (index >= pagination.end) ? 'hide' : '' ) ]">
                            <td class="timestamp hasTooltip">
                                <span>
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
                                </span>
                            </td>
                            <td :class="op.data.spec.op" class="operationType">
                                <span>
                                    {{ op.data.spec.op }}
                                </span>
                            </td>
                            <td class="baseHide opName hasTooltip">
                                <span>
                                    {{ op.data.metadata.name }}
                                </span>
                            </td>
                            <td class="phase center baseHide">
                                <span :class="getOpStatus(op)">
                                    {{ getOpStatus(op) }}
                                </span>
                            </td>
                            <td class="baseHide targetCluster hasTooltip">
                                <span>
                                    {{ op.data.spec.sgCluster }}
                                </span>
                            </td>
                            <td class="timestamp baseHide elapsed textRight hasTooltip">
                                <span class="time" v-if="op.data.hasOwnProperty('status')" v-html="getElapsedTime(op)"></span>
                            </td>
                            <td class="baseHide retries textRight">
                                <span>
                                    {{ (op.data.hasOwnProperty('status') && op.data.status.hasOwnProperty('opRetries')) ? op.data.status.opRetries : '0' }}
                                </span>
                                / 
                                <span>
                                    {{ op.data.spec.hasOwnProperty('maxRetries') ? op.data.spec.maxRetries : '1' }}
                                </span>
                            </td>
                            <td class="baseHide timedOut textRight">
                                {{ hasTimedOut(op) }}
                            </td>
                            <td class="actions textRight">
                                <a class="delete" title="Delete Configuration" @click="deleteCRD('sgdbops',$route.params.namespace, op.name)">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
                                </a>
                            </td>
                        </tr>

                        <tr class="details" :class="[($route.params.hasOwnProperty('name') && ( $route.params.name == op.name) ) ? 'open' : '', ( (index < pagination.start) || (index >= pagination.end) ? 'hide' : '' ) ]">
                            <td colspan="999">
                                <div class="opSpec">
                                    <table>
                                        <thead>
                                            <th colspan="3" class="label">
                                                General Operation Specs
                                            </th>
                                        </thead>

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

                                    
                                    <table>
                                        <thead>
                                            <th colspan="999" class="label">
                                                {{ op.data.spec.op }} Specs
                                            </th>
                                        </thead>
                                        
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
                                        <table>
                                            <thead>
                                                <th colspan="3" class="label">
                                                    {{ op.data.spec.op }} Databases
                                                </th>
                                            </thead>
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
                                        <table>
                                            <thead>
                                                <th colspan="3" class="label">
                                                    {{ op.data.spec.op }} Databases
                                                </th>
                                            </thead>
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
        
                                </div>

                                <div class="opStatus" v-if="op.data.hasOwnProperty('status')">
                                    <table class="opConditions">
                                        <thead>
                                            <th colspan="3" class="label">
                                                {{ op.data.spec.op }} Conditions
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions')"></span> 
                                            </th>
                                        </thead>
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

                                    <table>
                                        <thead>
                                            <th colspan="3" class="label">
                                                    {{ op.data.spec.op }} Status
                                            </th>
                                        </thead>
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
                                </div>
                            </td>
                        </tr>
                    </template>
                </tbody>
            </table>
        </div>
        <v-page :key="'pagination-'+pagination.rows" v-if="pagination.rows < dbOps.length" v-model="pagination.current" :page-size-menu="(pagination.rows > 1) ? [ pagination.rows, pagination.rows*2, pagination.rows*3 ] : [1]" :total-row="dbOps.length" @page-change="pageChange" align="center" ref="page"></v-page>
        <div id="nameTooltip">
            <div class="info"></div>
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
    td.operationType span {
        color: var(--baseColor);
        border: 1px solid var(--baseColor);
        border-radius: 15px;
        padding: 2px 10px;
        position: relative;
        top: -2px;
            white-space: nowrap;
    }
    td.operationType span:before {
        width: 15px;
        height: 15px;
        display: inline-block;
        content: "";
        position: relative;
        top: 3px;
        left: -3px;
    }
    td.operationType.benchmark span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+CjxnPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEyLjUsNy45Yy0wLjEtMC4zLTAuMi0wLjUtMC4zLTAuOGwtMS41LDAuN0MxMC44LDgsMTAuOSw4LjMsMTEsOC42TDEyLjUsNy45eiIvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTQuNyw3LjNDNC44LDcuMSw1LDYuOSw1LjIsNi43TDQuMSw1LjVDMy44LDUuNywzLjcsNS45LDMuNSw2LjFMNC43LDcuM3oiLz4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik01LjcsNi40QzYsNi4yLDYuMiw2LjEsNi41LDZMNS44LDQuNEM1LjYsNC41LDUuMyw0LjYsNS4xLDQuOEw1LjcsNi40eiIvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTQsOC42YzAuMS0wLjMsMC4yLTAuNSwwLjMtMC44TDIuOCw3LjJDMi42LDcuNCwyLjUsNy43LDIuNSw4TDQsOC42eiIvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTkuOSw0LjdDOS43LDQuNiw5LjQsNC41LDkuMSw0LjRMOC41LDZDOC44LDYuMSw5LDYuMiw5LjMsNi40TDkuOSw0Ljd6Ii8+Cgk8cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNMTEuNSw2LjFjLTAuMi0wLjItMC40LTAuNC0wLjYtMC42TDkuOCw2LjdjMC4yLDAuMiwwLjQsMC40LDAuNiwwLjZMMTEuNSw2LjF6Ii8+Cgk8cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNNy45LDQuMmMtMC4zLDAtMC41LDAtMC44LDB2MS43YzAuMywwLDAuNSwwLDAuOCwwVjQuMnoiLz4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik03LjUsMTMuMWMtMC44LDAtMS41LTAuNy0xLjUtMS41YzAtMC41LDAuMy0xLDAuOC0xLjNsMC43LTMuNWwwLjcsMy41YzAuNywwLjQsMSwxLjMsMC42LDIKCQlDOC41LDEyLjgsOCwxMy4xLDcuNSwxMy4xeiIvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTE1LDkuNGMwLDEuMS0wLjIsMi4xLTAuNywzLjFjLTAuMSwwLjMtMC41LDAuNS0wLjgsMC4zYzAsMCwwLDAtMC4xLDBsMCwwCgkJYy0wLjMtMC4yLTAuNC0wLjUtMC4zLTAuOGMxLjUtMy4xLDAuMS02LjgtMy04LjNzLTYuOC0wLjEtOC4zLDNjLTAuOCwxLjctMC44LDMuNiwwLDUuM2MwLjEsMC4zLDAsMC42LTAuMywwLjhsMCwwCgkJYy0wLjMsMC4yLTAuNywwLjEtMC45LTAuMmMwLDAsMCwwLDAtMC4xQy0xLDguNywwLjYsNC4zLDQuNCwyLjZjMS4xLTAuNSwyLjItMC43LDMuNC0wLjdDMTEuOCwyLDE1LDUuNCwxNSw5LjR6Ii8+CjwvZz4KPC9zdmc+);
        top: 2px;
    }
    td.operationType.minorVersionUpgrade span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+CjxnIHRyYW5zZm9ybT0idHJhbnNsYXRlKC0xMDQuODc0IC02OC41NTcpIj4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0xMTIuMSw3NC41bC0yLjIsMi4yYy0wLjEsMC4yLTAuMSwwLjQsMCwwLjVsMCwwbDAuNCwwLjQKCQljMC4yLDAuMSwwLjQsMC4xLDAuNSwwbDAsMGwxLjUtMS41bDEuNSwxLjVjMC4yLDAuMSwwLjQsMC4xLDAuNSwwbDAsMGwwLjQtMC40YzAuMS0wLjEsMC4xLTAuNCwwLTAuNWwwLDBsLTIuMi0yLjIKCQlDMTEyLjUsNzQuNCwxMTIuMyw3NC40LDExMi4xLDc0LjVDMTEyLjEsNzQuNSwxMTIuMSw3NC41LDExMi4xLDc0LjVMMTEyLjEsNzQuNXoiLz4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0xMTIuNCw4Mi4yYy0zLjQsMC02LjEtMi43LTYuMS02LjFjMC0zLjQsMi43LTYuMSw2LjEtNi4xYzMuNCwwLDYuMSwyLjcsNi4xLDYuMQoJCUMxMTguNSw3OS40LDExNS44LDgyLjIsMTEyLjQsODIuMnogTTExMi40LDcxLjFjLTIuNywwLTUsMi4yLTUsNWMwLDIuNywyLjIsNSw1LDVjMi43LDAsNS0yLjIsNS01YzAsMCwwLDAsMCwwCgkJQzExNy4zLDczLjMsMTE1LjEsNzEuMSwxMTIuNCw3MS4xTDExMi40LDcxLjF6Ii8+CjwvZz4KPC9zdmc+);
    }
    td.operationType.majorVersionUpgrade span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+CjxnIHRyYW5zZm9ybT0idHJhbnNsYXRlKC0xMjIuODc1IC02OS4zOTIpIj4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0xMzAuNiw3Ni45bDIuMiwyLjJjMC4xLDAuMiwwLjEsMC40LDAsMC41bDAsMGwtMC40LDAuNAoJCWMtMC4yLDAuMS0wLjQsMC4xLTAuNSwwbDAsMGwtMS42LTEuNmwtMS41LDEuNWMtMC4yLDAuMS0wLjQsMC4xLTAuNSwwbDAsMGwtMC40LTAuNGMtMC4xLTAuMi0wLjEtMC40LDAtMC41bDAsMGwyLjItMi4yCgkJQzEzMC4yLDc2LjcsMTMwLjUsNzYuNywxMzAuNiw3Ni45TDEzMC42LDc2Ljl6IE0xMzAuMSw3My44bC0yLjIsMi4yYy0wLjEsMC4yLTAuMSwwLjQsMCwwLjVsMCwwbDAuNCwwLjRjMC4yLDAuMSwwLjQsMC4xLDAuNSwwCgkJbDAsMGwxLjUtMS41bDEuNSwxLjVjMC4yLDAuMSwwLjQsMC4xLDAuNSwwbDAsMGwwLjQtMC40YzAuMS0wLjIsMC4xLTAuNCwwLTAuNWwwLDBsLTIuMi0yLjJDMTMwLjUsNzMuNiwxMzAuMiw3My42LDEzMC4xLDczLjgKCQlDMTMwLjEsNzMuOCwxMzAuMSw3My44LDEzMC4xLDczLjhMMTMwLjEsNzMuOHoiLz4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0xMzAuNCw4M2MtMy40LDAtNi4xLTIuNy02LjEtNi4xczIuNy02LjEsNi4xLTYuMWMzLjQsMCw2LjEsMi43LDYuMSw2LjEKCQlDMTM2LjUsODAuMywxMzMuNyw4MywxMzAuNCw4M3ogTTEzMC40LDcxLjljLTIuNywwLTUsMi4yLTUsNWMwLDIuNywyLjIsNSw1LDVjMi43LDAsNS0yLjIsNS01QzEzNS4zLDc0LjEsMTMzLjEsNzEuOSwxMzAuNCw3MS45CgkJTDEzMC40LDcxLjl6Ii8+CjwvZz4KPC9zdmc+);
    }
    td.operationType.repack span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+CjxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0xMi45LDQuNWMwLDAsMC0wLjEsMC0wLjFjMC0wLjEtMC4xLTAuMS0wLjEtMC4yYy0wLjEtMC4yLTAuMy0wLjMtMC41LTAuNUw4LjQsMS42Yy0wLjUtMC4zLTEuMi0wLjMtMS43LDAKCUwyLjgsMy44QzIuNiwzLjksMi40LDQuMSwyLjMsNC4zYzAsMC0wLjEsMC4xLTAuMSwwLjFjMCwwLDAsMC4xLDAsMC4xQzIsNC43LDEuOSw1LDEuOSw1LjN2NC40YzAsMC42LDAuMywxLjIsMC45LDEuNWwzLjgsMi4yCgljMC4yLDAuMSwwLjQsMC4yLDAuNiwwLjJjMC4xLDAuMSwwLjIsMC4xLDAuMywwLjFjMC4xLDAsMC4yLDAsMC4zLTAuMWMwLjIsMCwwLjQtMC4xLDAuNi0wLjJsMy44LTIuMmMwLjUtMC4zLDAuOS0wLjksMC45LTEuNQoJYzAsMCwwLDAsMCwwVjUuM0MxMy4xLDUsMTMsNC44LDEyLjksNC41eiBNNy4zLDIuN2MwLjEsMCwwLjItMC4xLDAuMi0wLjFzMC4yLDAsMC4yLDAuMWwzLjQsMS45TDcuNSw2LjhMMy45LDQuNkw3LjMsMi43egoJIE0zLjQsMTAuMUMzLjMsMTAsMy4yLDkuOCwzLjIsOS43VjUuN2wzLjcsMi4xVjEyTDMuNCwxMC4xeiBNMTEuNiwxMC4xbC0zLjQsMlY3LjhsMy43LTIuMXYzLjlDMTEuOCw5LjgsMTEuNywxMCwxMS42LDEwLjF6Ii8+Cjwvc3ZnPg==);
    }
    td.operationType.restart span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+CjxnPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEzLDguNmMwLjQtMS44LTAuMS0zLjctMS41LTUuMWMtMS4yLTEuMi0yLjgtMS44LTQuNC0xLjZMNi43LDEuMWMwLDAsMC0wLjEtMC4xLTAuMUM2LjYsMSw2LjUsMSw2LjQsMS4xCgkJTDQuNiwzLjZjMCwwLDAsMC4xLDAsMC4xYzAsMC4xLDAuMSwwLjIsMC4yLDAuMmwzLjEsMC4zYzAsMCwwLjEsMCwwLjEsMEM4LDQuMiw4LjEsNC4xLDgsNEw3LjcsMy4zYzEsMCwyLDAuNSwyLjgsMS4yCgkJYzEsMC45LDEuNCwyLjMsMS4yLDMuNWMtMC4zLDAuMS0wLjcsMC4zLTAuOSwwLjZjMCwwLDAsMCwwLDBjLTAuNSwwLjYtMC4zLDEuNSwwLjQsMmMwLjYsMC41LDEuNSwwLjMsMi0wLjQKCQlDMTMuNSw5LjcsMTMuNCw5LDEzLDguNnoiLz4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0xMC4zLDExLjFsLTMuMS0wLjNjMCwwLTAuMSwwLTAuMSwwQzcsMTAuOCw2LjksMTAuOSw3LDExbDAuMywwLjdjLTEsMC0yLTAuNS0yLjgtMS4yCgkJQzMuNSw5LjUsMy4xLDguMiwzLjQsN0MzLjcsNi45LDQsNi43LDQuMiw2LjRjMCwwLDAsMCwwLDBjMC41LTAuNiwwLjMtMS41LTAuNC0yYy0wLjYtMC41LTEuNS0wLjMtMiwwLjRDMS41LDUuMywxLjYsNiwyLDYuNQoJCWMtMC40LDEuOCwwLjEsMy43LDEuNSw1LjFjMSwxLjEsMi40LDEuNiwzLjksMS42aDBjMC4yLDAsMC4zLDAsMC41LDBsMC4zLDAuOGMwLDAsMCwwLjEsMC4xLDAuMWMwLjEsMC4xLDAuMiwwLDAuMywwbDEuOC0yLjUKCQljMCwwLDAtMC4xLDAtMC4xQzEwLjUsMTEuMiwxMC40LDExLjEsMTAuMywxMS4xeiIvPgo8L2c+Cjwvc3ZnPg==);
    }
    td.operationType.vacuum span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+CjxnPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTcuNCw2LjFDNy43LDYuMSw4LDUuOSw4LDUuNlYyLjFjMC0wLjMtMC4yLTAuNS0wLjUtMC41Yy0wLjMsMC0wLjUsMC4yLTAuNSwwLjV2My41CgkJQzYuOSw1LjksNy4yLDYuMSw3LjQsNi4xeiIvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTUuNCw1LjZjMCwwLjMsMC4yLDAuNSwwLjUsMC41YzAuMywwLDAuNS0wLjIsMC41LTAuNWMwLjEtMS41LTAuNS0zLTEuNy0zLjlDNC41LDEuNSw0LjIsMS42LDQsMS44CgkJUzMuOSwyLjQsNC4xLDIuNUM1LjEsMy4zLDUuNiw0LjQsNS40LDUuNnoiLz4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0zLjksNS42YzAsMC4zLDAuMiwwLjUsMC41LDAuNWMwLDAsMCwwLDAsMGMwLjMsMCwwLjUtMC4yLDAuNS0wLjVjMCwwLDAtMC4xLDAtMC4xCgkJQzQuOCwzLjYsMy4xLDIuMiwxLjIsMi4zYzAsMCwwLDAsMCwwYy0wLjMsMC0wLjUsMC4yLTAuNSwwLjVjMCwwLjMsMC4yLDAuNSwwLjUsMC41YzAuMSwwLDAuMSwwLDAuMiwwQzIuNywzLjMsMy44LDQuMywzLjksNS42eiIKCQkvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTkuMSw2LjFjMC4zLDAsMC41LTAuMiwwLjUtMC41Yy0wLjEtMS4yLDAuNC0yLjQsMS4zLTMuMWMwLjItMC4yLDAuMy0wLjUsMC4xLTAuN3MtMC41LTAuMy0wLjctMC4xCgkJQzkuMSwyLjYsOC40LDQuMSw4LjUsNS42QzguNSw1LjksOC44LDYuMSw5LjEsNi4xeiIvPgoJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEzLjgsMi4zYzAsMC0wLjEsMC0wLjEsMGMtMS45LTAuMS0zLjUsMS40LTMuNiwzLjNjMCwwLjMsMC4yLDAuNSwwLjUsMC41YzAuMywwLDAuNS0wLjIsMC41LTAuNQoJCWMwLTAuMSwwLTAuMSwwLTAuMmMwLjItMS4zLDEuNC0yLjMsMi43LTIuMWMwLDAsMCwwLDAsMGMwLjMsMCwwLjUtMC4yLDAuNS0wLjVDMTQuMywyLjUsMTQuMSwyLjMsMTMuOCwyLjN6Ii8+Cgk8cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNMTMsNi41SDJjLTAuMywwLTAuNSwwLjItMC41LDAuNXYxLjJjMCwxLjIsMSwyLjMsMi4zLDIuM0g1YzAsMC42LDAuNSwxLjIsMS4yLDEuMkg3djEuMgoJCWMwLDAuMywwLjIsMC41LDAuNSwwLjVTOCwxMy4yLDgsMTIuOXYtMS4yaDAuOGMwLjYsMCwxLjItMC41LDEuMi0xLjJoMS4yYzEuMiwwLDIuMy0xLDIuMy0yLjNWNy4xQzEzLjUsNi44LDEzLjMsNi41LDEzLDYuNXoKCQkgTTguOSwxMC42SDYuMWMtMC4xLDAtMC4xLTAuMS0wLjEtMC4xaDNDOSwxMC42LDguOSwxMC42LDguOSwxMC42eiBNMTIuNSw4LjJjMCwwLjctMC41LDEuMi0xLjIsMS4ySDkuNWMwLDAsMCwwLDAsMEg1LjUKCQljMCwwLDAsMCwwLDBIMy43Yy0wLjcsMC0xLjItMC41LTEuMi0xLjJWNy42bDEwLDBWOC4yeiIvPgo8L2c+Cjwvc3ZnPg==);
    }
    td.operationType.securityUpgrade span:before {
        background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSIgPgo8ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgtODIuNzQgLTExMi4wMTEpIj4KCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik05NS44LDEyMy45SDg0LjdjLTAuNywwLTEuMi0wLjYtMS4yLTEuM3YtNi4zYzAtMC43LDAuNS0xLjMsMS4yLTEuM2gxMS4yCgkJYzAuNywwLDEuMiwwLjYsMS4yLDEuM3Y2LjNDOTcsMTIzLjMsOTYuNSwxMjMuOSw5NS44LDEyMy45eiBNODQuNywxMTYuMWMtMC4xLDAtMC4yLDAuMS0wLjIsMC4zdjYuM2MwLDAuMSwwLjEsMC4zLDAuMiwwLjNoMTEuMgoJCWMwLjEsMCwwLjItMC4xLDAuMi0wLjN2LTYuM2MwLTAuMS0wLjEtMC4zLTAuMi0wLjNIODQuN3oiLz4KCTxnIHRyYW5zZm9ybT0idHJhbnNsYXRlKDg1LjQ2NyAxMTMuOTU2KSI+CgkJPHBhdGggZmlsbD0iIzM2QThGRiIgZD0iTTEuMiw4QzEsOCwwLjgsNy44LDAuOCw3LjZ2LTRjMC0wLjMsMC4yLTAuNSwwLjUtMC40YzAuMiwwLDAuNCwwLjIsMC40LDAuNHY0CgkJCUMxLjcsNy44LDEuNSw4LDEuMiw4QzEuMiw4LDEuMiw4LDEuMiw4eiIvPgoJCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik0zLjYsOEMzLjMsOCwzLjEsNy44LDMuMSw3LjZ2LTRjMC0wLjMsMC4yLTAuNSwwLjUtMC40QzMuOCwzLjEsNCwzLjMsNCwzLjZ2NAoJCQlDNC4xLDcuOCwzLjgsOCwzLjYsOEMzLjYsOCwzLjYsOCwzLjYsOHoiLz4KCQk8cGF0aCBmaWxsPSIjMzZBOEZGIiBkPSJNNiw4QzUuNyw4LDUuNSw3LjgsNS41LDcuNnYtNGMwLTAuMywwLjItMC41LDAuNS0wLjRjMC4yLDAsMC40LDAuMiwwLjQsMC40djQKCQkJQzYuNCw3LjgsNi4yLDgsNiw4QzYsOCw2LDgsNiw4eiIvPgoJCTxwYXRoIGZpbGw9IiMzNkE4RkYiIGQ9Ik04LjMsOEM4LjEsOCw3LjksNy44LDcuOSw3LjZ2LTRjMC0wLjMsMC4yLTAuNSwwLjQtMC41YzAuMywwLDAuNSwwLjIsMC41LDAuNGMwLDAsMCwwLDAsMAoJCQl2NEM4LjgsNy44LDguNiw4LDguMyw4QzguMyw4LDguMyw4LDguMyw4eiIvPgoJPC9nPgo8L2c+Cjwvc3ZnPg==);
    }

    .opSpec, .opStatus {
        width: 50%;
        float: left;
    }

    .opSpec th, .opStatus th {
        text-transform: capitalize;
    }

    tr.details table {
        margin-bottom: 12px;
    }

    td.elapsed, td.retries, td.timedOut {
        padding-right: 5px;
    }

    td.phase > span.Completed {
        color: #03CC03;
        background: #00F90040;
    }

    td.phase > span.Running {
        color: #DE9826;
        background: #FCC12040;
    }

    td.phase > span.Failed {
        color: #FF2600;
        background: #FF260040;
    }

    tr.base.open .baseHide {
        opacity: 0;
    }

    th.actions, td.actions {
        width: 45px !important;
        min-width: 45px;
        max-width: 45px;
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