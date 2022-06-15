<template>
    <div id="dbops" v-if="iCanLoad">
        <div class="content">
            <template v-if="!$route.params.hasOwnProperty('name')">
                <div class="toolbar">
                    <div class="searchBar">
                        <input id="keyword" v-model="filters.keyword" class="search" placeholder="Search by name..." autocomplete="off">
                        <a @click="filterOps('keyword')" class="btn">APPLY</a>
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
                
                <table id="sgdbops" class="dbops resizable fullWidth" v-columns-resizable>
                    <thead class="sort">
                        <th class="asc start hasTooltip" data-type="timestamp">
                            <span @click="sort('data.spec.runAt', 'timestamp')" title="Start">Start</span>
                            <span class="helpTooltip" :data-tooltip="(timezone == 'local') ? getTooltip('sgdbops.spec.runAt').replace('UTC ','') : getTooltip('sgdbops.spec.runAt')"></span>
                        </th>
                        <th class="asc operationType" data-type="type">
                            <span @click="sort('data.spec.op')" title="Operation">Operation</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.op')"></span>
                        </th>
                        <th class="asc opName hasTooltip">
                            <span @click="sort('data.metadata.name')" title="Name">Name</span>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.name')"></span>
                        </th>
                        <th class="asc phase hasTooltip" data-type="phase">
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
                        <th class="asc timedOut textRight hasTooltip" data-type="timedOut">
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
                                    <td class="operationType">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            <span class="dbopIcon" :class="op.data.spec.op">
                                                {{ op.data.spec.op }}
                                            </span>
                                        </router-link>                                        
                                    </td>
                                    <td class="opName hasTooltip">
                                        <span>
                                            <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                                {{ op.data.metadata.name }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="phase center">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor" :class="getOpStatus(op).toLowerCase()" :set="opStatus = getOpStatus(op)">
                                            <span :class="opStatus">                                            
                                                {{opStatus }}
                                            </span>
                                            <span v-if="( hasProp(op, 'data.status.' + op.data.spec.op + '.failure') && (opStatus == 'Failed') )" class="helpTooltip failed onHover" :data-tooltip="op.data.status[op.data.spec.op].failure"></span>
                                        </router-link>
                                    </td>
                                    <td class="targetCluster hasTooltip">
                                        <span>
                                            <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                                {{ op.data.spec.sgCluster }}
                                            </router-link>
                                        </span>
                                    </td>
                                    <td class="timestamp elapsed textRight hasTooltip">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            <span class="time" v-if="op.data.hasOwnProperty('status')" v-html="getElapsedTime(op)"></span>
                                        </router-link>
                                    </td>
                                    <td class="retries textRight">
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
                                    <td class="timedOut textRight">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" class="noColor">
                                            {{ hasTimedOut(op) }}
                                        </router-link>
                                    </td>
                                    <td class="actions textRight">
                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + op.data.metadata.name" target="_blank" class="newTab"></router-link>
                                        <a class="delete deleteCRD" title="Delete Operation" @click="deleteCRD('sgdbops',$route.params.namespace, op.name)"></a>
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

            <template v-else-if="$route.params.hasOwnProperty('name') && !$route.params.hasOwnProperty('uid')">
                <h2>Operation Details</h2>
                <template v-for="(op, index) in dbOps" v-if="op.name == $route.params.name">
                    <template v-if="( (op.data.spec.op == 'minorVersionUpgrade') && hasProp(op, 'data.status.conditions') )">
                        <div class="clusterStatus" v-for="status in op.data.status.conditions" v-if="( (['Running','Completed'].includes(status.type)) && (status.status == 'True') )">
                            <div class="upgradeLog">
                                 <h3 class="header xsPad relative">
                                    Upgrade Log
                                </h3>
                                <table v-if="op.data.status.hasOwnProperty('opStarted')">
                                    <tbody>
                                        <tr>
                                            <td class="timestamp">
                                                <span class='date'>
                                                    {{ op.data.status.opStarted | formatTimestamp('date') }}
                                                </span>
                                                <span class='time'>
                                                    {{ op.data.status.opStarted | formatTimestamp('time') }}
                                                </span>
                                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                                            </td>
                                            <td>
                                                The operation has started
                                            </td>
                                        </tr>
                                        <tr v-if="hasProp(op, 'data.status.minorVersionUpgrade.switchoverInitiated')">
                                            <td class="timestamp">
                                                <span class='date'>
                                                    {{ op.data.status.minorVersionUpgrade.switchoverInitiated | formatTimestamp('date') }}
                                                </span>
                                                <span class='time'>
                                                    {{ op.data.status.minorVersionUpgrade.switchoverInitiated | formatTimestamp('time') }}
                                                </span>
                                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                                            </td>
                                            <td>Switchover has been initiated</td>
                                        </tr>
                                        <tr v-if="hasProp(op, 'data.status.minorVersionUpgrade.switchoverFinalized')">
                                            <td class="timestamp">
                                                <span class='date'>
                                                    {{ op.data.status.minorVersionUpgrade.switchoverFinalized | formatTimestamp('date') }}
                                                </span>
                                                <span class='time'>
                                                    {{ op.data.status.minorVersionUpgrade.switchoverFinalized | formatTimestamp('time') }}
                                                </span>
                                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                                            </td>
                                            <td>Switchover has finalized</td>
                                        </tr>
                                        <tr v-for="condition in op.data.status.conditions" v-if="( (condition.status == 'True') && (condition.type != 'Running') )">
                                            <td class="timestamp">
                                                <span class='date'>
                                                    {{ condition.lastTransitionTime | formatTimestamp('date') }}
                                                </span>
                                                <span class='time'>
                                                    {{ condition.lastTransitionTime | formatTimestamp('time') }}
                                                </span>
                                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                                            </td>
                                            <td>
                                                The operation has {{ condition.type.toLowerCase() }}
                                            </td>
                                        </tr>
                                    </tbody>
                                </table>
                            </div>

                            <template v-if="(status.type == 'Running')">
                                <h3 class="header xsPad relative">
                                    Upgrade Overview

                                    <span class="stopWatch time">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 75 75">
                                            <path fill="#A6A7AE" transform="translate(-4.5 -2.25)" d="M38,48.2h8.1V26.4H38V48.2z M67.7,25.5
                                            l5.2-5.2l-5.7-5.7L62,19.8c-14.2-10.9-34.6-8.4-45.6,5.6s-8.5,34.2,5.7,45.1s34.6,8.4,45.6-5.6C76.8,53.2,76.8,37,67.7,25.5z
                                            M60,62.8c-9.9,9.8-26,9.8-35.9,0s-9.9-25.7,0-35.5s26-9.8,35.9,0c4.8,4.7,7.4,11.1,7.4,17.8C67.4,51.8,64.8,58.2,60,62.8L60,62.8z
                                            M31.2,2.2h21.7v8H31.2V2.2z"/>
                                        </svg>
                                        <span v-for="time in (3 - getElapsedTime(op).split(' ').length)">
                                            00
                                        </span>
                                        <span v-for="time in getElapsedTime(op).split(' ')">
                                            {{ (time.match(/\d+/)[0].length > 1) ? time.match(/\d+/)[0] : '0'+time.match(/\d+/)[0] }}
                                        </span>
                                    </span>
                                </h3>

                                <template v-for="cluster in clusters" v-if="cluster.name == op.data.spec.sgCluster">
                                    <div class="pods" v-if="( (cluster.status.podsReady > 0) && hasProp(op, 'data.status.minorVersionUpgrade') )">
                                        <template v-for="(initialPod, index) in op.data.status.minorVersionUpgrade.initialInstances">
                                            <template v-if="cluster.status.pods.filter(p => (p.name == initialPod)).length">
                                                <template v-for="pod in cluster.status.pods" v-if="( (pod.name == initialPod) || ( (index ==  op.data.status.minorVersionUpgrade.initialInstances.length - 1) && !op.data.status.minorVersionUpgrade.initialInstances.includes(pod.name)) )">
                                                    <div class="pod" :class="[pod.role, pod.status]">
                                                        <div class="podInfo xsPad">
                                                            <strong class="podName">
                                                                {{ pod.name }}
                                                            </strong>
                                                            <div class="podStatus floatRight" v-if="pod.hasOwnProperty('role')">
                                                                <span class="label capitalize" :class="pod.role">
                                                                    <span>{{ (!op.data.status.minorVersionUpgrade.initialInstances.includes(pod.name) && (status.type == 'Running')) ? 'Temporary Replica' : pod.role }}</span>
                                                                </span>
                                                            </div>
                                                        </div>
                                                        <div class="podStatus">
                                                            <div class="repStats flex flex-50 xsPad">
                                                                <div class="nodeIcon">
                                                                    <svg xmlns="http://www.w3.org/2000/svg" width="38.311" height="43.788" viewBox="0 0 38.311 43.788">
                                                                        <g transform="translate(-129.63 -69.152)">
                                                                            <g class="percent-75-100" :class="(primaryNodeDisk && ((getBytes(pod.diskUsed) * 100 / primaryNodeDisk) > 85) ) ? 'full' : ''" transform="translate(0 0)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 84.57740783691406 C 143.7403259277344 84.57740783691406 139.0069885253906 83.9876708984375 135.4573059082031 82.91683197021484 C 132.3255157470703 81.97205352783203 130.3799896240234 80.68137359619141 130.3799896240234 79.54847717285156 L 130.3799896240234 74.930908203125 C 130.3799896240234 73.7979736328125 132.3253326416016 72.50729370117188 135.4568634033203 71.56256103515625 C 139.0063629150391 70.49172210693359 143.7398681640625 69.90199279785156 148.7854461669922 69.90199279785156 C 153.8305816650391 69.90199279785156 158.5639343261719 70.49172973632812 162.1136169433594 71.56256103515625 C 165.2454071044922 72.50733947753906 167.19091796875 73.79801177978516 167.19091796875 74.930908203125 L 167.19091796875 79.54847717285156 C 167.19091796875 80.68137359619141 165.2454071044922 81.97205352783203 162.1136169433594 82.91683197021484 C 158.5639343261719 83.9876708984375 153.83056640625 84.57740783691406 148.7854461669922 84.57740783691406 Z" stroke="none"/>
                                                                                <path class="out" d="M 148.7854461669922 70.65199279785156 C 143.8112487792969 70.65199279785156 139.1546630859375 71.23036956787109 135.6734771728516 72.28059387207031 C 131.9647064208984 73.39948272705078 131.1299896240234 74.59893798828125 131.1299896240234 74.930908203125 L 131.1299896240234 79.54847717285156 C 131.1299896240234 79.88043212890625 131.9647827148438 81.07984161376953 135.6739196777344 82.19879150390625 C 139.1552734375 83.2490234375 143.8116912841797 83.82740783691406 148.7854461669922 83.82740783691406 C 153.7592163085938 83.82740783691406 158.4156494140625 83.2490234375 161.8969879150391 82.19879150390625 C 165.6061248779297 81.07984924316406 166.44091796875 79.88043212890625 166.44091796875 79.54847717285156 L 166.44091796875 74.930908203125 C 166.44091796875 74.59896087646484 165.6061248779297 73.39954376220703 161.8969879150391 72.28060150146484 C 158.4156494140625 71.23036956787109 153.7592163085938 70.65199279785156 148.7854461669922 70.65199279785156 M 148.7854461669922 69.15199279785156 C 159.3642120361328 69.15199279785156 167.94091796875 71.73909759521484 167.94091796875 74.930908203125 L 167.94091796875 79.54847717285156 C 167.94091796875 82.74028015136719 159.3642120361328 85.32740783691406 148.7854461669922 85.32740783691406 C 138.2067260742188 85.32740783691406 129.6299896240234 82.74028015136719 129.6299896240234 79.54847717285156 L 129.6299896240234 74.930908203125 C 129.6299896240234 71.73909759521484 138.2055053710938 69.15199279785156 148.7854461669922 69.15199279785156 Z" stroke="none"/>
                                                                            </g>
                                                                            <g class="percent-50-75" :class="(primaryNodeDisk && ((getBytes(pod.diskUsed) * 100 / primaryNodeDisk) > 65) ) ? 'full' : ''" transform="translate(0 2.919)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 90.86602783203125 C 143.7403259277344 90.86602783203125 139.0069885253906 90.27628326416016 135.4573059082031 89.2054443359375 C 132.3255157470703 88.26066589355469 130.3799896240234 86.96998596191406 130.3799896240234 85.83709716796875 L 130.3799896240234 81.5208740234375 C 131.9980316162109 82.82033538818359 134.6211547851562 83.65998840332031 136.6966552734375 84.15798187255859 C 140.1705322265625 84.99150848388672 144.3507690429688 85.43207550048828 148.7854461669922 85.43207550048828 C 153.2197265625 85.43207550048828 157.400146484375 84.99167633056641 160.874755859375 84.15848541259766 C 162.9495697021484 83.66095733642578 165.5720977783203 82.82216644287109 167.19091796875 81.52445983886719 L 167.19091796875 85.83709716796875 C 167.19091796875 86.96998596191406 165.2454071044922 88.26066589355469 162.1136169433594 89.2054443359375 C 158.5639343261719 90.27628326416016 153.83056640625 90.86602783203125 148.7854461669922 90.86602783203125 Z" stroke="none"/>
                                                                                <path class="out" d="M 166.44091796875 82.91915893554688 C 164.7964172363281 83.84923553466797 162.7451477050781 84.48123168945312 161.0496520996094 84.88780212402344 C 157.5186157226562 85.73452758789062 153.2777252197266 86.18207550048828 148.7854461669922 86.18207550048828 C 144.292724609375 86.18207550048828 140.0519866943359 85.73434448242188 136.5216674804688 84.88728332519531 C 134.2853240966797 84.35069274902344 132.4831848144531 83.69454956054688 131.1299896240234 82.92610168457031 L 131.1299896240234 85.83709716796875 C 131.1299896240234 86.16904449462891 131.9647827148438 87.36845397949219 135.6739196777344 88.48740386962891 C 139.1552734375 89.53763580322266 143.8116912841797 90.11602783203125 148.7854461669922 90.11602783203125 C 153.7592163085938 90.11602783203125 158.4156494140625 89.53763580322266 161.8969879150391 88.48740386962891 C 165.6061248779297 87.36846160888672 166.44091796875 86.16904449462891 166.44091796875 85.83709716796875 L 166.44091796875 82.91915893554688 M 129.9355621337891 80.16232299804688 C 130.0228881835938 80.16232299804688 130.1120910644531 80.19865417480469 130.1776885986328 80.27956390380859 C 132.224365234375 82.80720520019531 139.7789916992188 84.68207550048828 148.7854461669922 84.68207550048828 C 157.7931365966797 84.68207550048828 165.3465576171875 82.80720520019531 167.3944702148438 80.28335571289062 C 167.4601287841797 80.20194244384766 167.5491943359375 80.16549682617188 167.6363677978516 80.16550445556641 C 167.7916564941406 80.16551971435547 167.94091796875 80.28116607666016 167.94091796875 80.46427154541016 L 167.94091796875 85.83709716796875 C 167.94091796875 89.02889251708984 159.3642120361328 91.61602783203125 148.7854461669922 91.61602783203125 C 138.2067260742188 91.61602783203125 129.6299896240234 89.02889251708984 129.6299896240234 85.83709716796875 L 129.6299896240234 80.46174621582031 C 129.6299896240234 80.27775573730469 129.7798461914062 80.16232299804688 129.9355621337891 80.16232299804688 Z" stroke="none"/>
                                                                            </g>
                                                                            <g class="percent-25-50" :class="(primaryNodeDisk && ((getBytes(pod.diskUsed) * 100 / primaryNodeDisk) > 35) ) ? 'full' : ''" transform="translate(0 4.847)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 98.13986206054688 C 143.7405242919922 98.13986206054688 139.0071563720703 97.55018615722656 135.457275390625 96.47944641113281 C 132.3254852294922 95.53481292724609 130.3799896240234 94.24456787109375 130.3799896240234 93.11220550537109 L 130.3799896240234 88.79533386230469 C 131.9990081787109 90.09484100341797 134.6217041015625 90.93424987792969 136.6966857910156 91.43201446533203 C 140.1708068847656 92.26542663574219 144.3510284423828 92.7059326171875 148.7854461669922 92.7059326171875 C 153.2197265625 92.7059326171875 157.400146484375 92.26553344726562 160.874755859375 91.43234252929688 C 162.9495697021484 90.93482208251953 165.5720977783203 90.09603118896484 167.19091796875 88.79833221435547 L 167.19091796875 93.11220550537109 C 167.19091796875 94.24456787109375 165.2454223632812 95.53481292724609 162.1136474609375 96.47944641113281 C 158.5637664794922 97.55018615722656 153.8303833007812 98.13986206054688 148.7854461669922 98.13986206054688 Z" stroke="none"/>
                                                                                <path class="out" d="M 131.1299896240234 90.19075775146484 L 131.1299896240234 93.11220550537109 C 131.1299896240234 93.44392395019531 131.9647674560547 94.64263153076172 135.6738586425781 95.76140594482422 C 139.1554260253906 96.81153106689453 143.8118743896484 97.38986206054688 148.7854461669922 97.38986206054688 C 153.759033203125 97.38986206054688 158.4154968261719 96.81153106689453 161.8970489501953 95.76140594482422 C 165.6061401367188 94.64264678955078 166.44091796875 93.44392395019531 166.44091796875 93.11220550537109 L 166.44091796875 90.19303131103516 C 164.7964172363281 91.12310791015625 162.7451477050781 91.75510406494141 161.0496520996094 92.16167449951172 C 157.5186309814453 93.00838470458984 153.2777252197266 93.4559326171875 148.7854461669922 93.4559326171875 C 144.2929992675781 93.4559326171875 140.0522766113281 93.00826263427734 136.5217437744141 92.16132354736328 C 134.8262176513672 91.75458526611328 132.7748870849609 91.12217712402344 131.1299896240234 90.19075775146484 M 129.9354248046875 87.43683624267578 C 130.0227966308594 87.43683624267578 130.112060546875 87.47329711914062 130.1776885986328 87.55471801757812 C 132.224365234375 90.08107757568359 139.7789916992188 91.9559326171875 148.7854461669922 91.9559326171875 C 157.7931365966797 91.9559326171875 165.3465576171875 90.08107757568359 167.3944702148438 87.55722808837891 C 167.4601440429688 87.47623443603516 167.5492553710938 87.43991851806641 167.6364593505859 87.43992614746094 C 167.7917175292969 87.43994140625 167.94091796875 87.55506134033203 167.94091796875 87.73814392089844 L 167.94091796875 93.11220550537109 C 167.94091796875 96.30276489257812 159.3642120361328 98.88986206054688 148.7854461669922 98.88986206054688 C 138.2067260742188 98.88986206054688 129.6299896240234 96.30276489257812 129.6299896240234 93.11220550537109 L 129.6299896240234 87.735595703125 C 129.6299896240234 87.55247497558594 129.7797546386719 87.43683624267578 129.9354248046875 87.43683624267578 Z" stroke="none"/>
                                                                            </g>
                                                                            <g class="percent-0-25" :class="(primaryNodeDisk && ((getBytes(pod.diskUsed) * 100 / primaryNodeDisk) > 20) ) ? 'full' : ''" transform="translate(0 13.847)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 98.13986206054688 C 143.7405242919922 98.13986206054688 139.0071563720703 97.55018615722656 135.457275390625 96.47944641113281 C 132.3254852294922 95.53481292724609 130.3799896240234 94.24456787109375 130.3799896240234 93.11220550537109 L 130.3799896240234 88.79533386230469 C 131.9990081787109 90.09484100341797 134.6217041015625 90.93424987792969 136.6966857910156 91.43201446533203 C 140.1708068847656 92.26542663574219 144.3510284423828 92.7059326171875 148.7854461669922 92.7059326171875 C 153.2197265625 92.7059326171875 157.400146484375 92.26553344726562 160.874755859375 91.43234252929688 C 162.9495697021484 90.93482208251953 165.5720977783203 90.09603118896484 167.19091796875 88.79833221435547 L 167.19091796875 93.11220550537109 C 167.19091796875 94.24456787109375 165.2454223632812 95.53481292724609 162.1136474609375 96.47944641113281 C 158.5637664794922 97.55018615722656 153.8303833007812 98.13986206054688 148.7854461669922 98.13986206054688 Z" stroke="none"/>
                                                                                <path class="out" d="M 131.1299896240234 90.19075775146484 L 131.1299896240234 93.11220550537109 C 131.1299896240234 93.44392395019531 131.9647674560547 94.64263153076172 135.6738586425781 95.76140594482422 C 139.1554260253906 96.81153106689453 143.8118743896484 97.38986206054688 148.7854461669922 97.38986206054688 C 153.759033203125 97.38986206054688 158.4154968261719 96.81153106689453 161.8970489501953 95.76140594482422 C 165.6061401367188 94.64264678955078 166.44091796875 93.44392395019531 166.44091796875 93.11220550537109 L 166.44091796875 90.19303131103516 C 164.7964172363281 91.12310791015625 162.7451477050781 91.75510406494141 161.0496520996094 92.16167449951172 C 157.5186309814453 93.00838470458984 153.2777252197266 93.4559326171875 148.7854461669922 93.4559326171875 C 144.2929992675781 93.4559326171875 140.0522766113281 93.00826263427734 136.5217437744141 92.16132354736328 C 134.8262176513672 91.75458526611328 132.7748870849609 91.12217712402344 131.1299896240234 90.19075775146484 M 129.9354248046875 87.43683624267578 C 130.0227966308594 87.43683624267578 130.112060546875 87.47329711914062 130.1776885986328 87.55471801757812 C 132.224365234375 90.08107757568359 139.7789916992188 91.9559326171875 148.7854461669922 91.9559326171875 C 157.7931365966797 91.9559326171875 165.3465576171875 90.08107757568359 167.3944702148438 87.55722808837891 C 167.4601440429688 87.47623443603516 167.5492553710938 87.43991851806641 167.6364593505859 87.43992614746094 C 167.7917175292969 87.43994140625 167.94091796875 87.55506134033203 167.94091796875 87.73814392089844 L 167.94091796875 93.11220550537109 C 167.94091796875 96.30276489257812 159.3642120361328 98.88986206054688 148.7854461669922 98.88986206054688 C 138.2067260742188 98.88986206054688 129.6299896240234 96.30276489257812 129.6299896240234 93.11220550537109 L 129.6299896240234 87.735595703125 C 129.6299896240234 87.55247497558594 129.7797546386719 87.43683624267578 129.9354248046875 87.43683624267578 Z" stroke="none"/>
                                                                            </g>                                                                                                                        
                                                                        </g>
                                                                    </svg>
                                                                    <span v-if="( primaryNodeDisk && (pod.role != 'primary') && (pod.hasOwnProperty('diskUsed')) )" class="dataPercent">
                                                                        <strong>{{ Math.ceil(parseInt(getBytes(pod.diskUsed) * 100 / primaryNodeDisk)) }}%</strong><br/>
                                                                        Replication
                                                                    </span>
                                                                </div>
                                                                <div class="pgVersion" v-if="hasProp(pod, 'componentVersions.postgresql')">
                                                                    <span class="label">
                                                                        <span>{{ pod.componentVersions.postgresql }}</span>
                                                                    </span><br/>
                                                                    Postgres Version
                                                                </div>
                                                            </div>
                                                            <div class="podFooter xsPad">
                                                                <span class="connCount" v-if="pod.hasOwnProperty('connections')">
                                                                    <strong>
                                                                        {{ getPodConnections(pod.name, pod.connections) }}
                                                                    </strong></br>
                                                                    Connections
                                                                </span>
                                                                <span class="label status floatRight" :class="pod.status">
                                                                    <span>{{ pod.status }}</span>
                                                                </span>
                                                            </div>
                                                            <div class="connGraph chart-wrapper" title="Real Time Connections">
                                                                <apexchart 
                                                                    v-if="pod.hasOwnProperty('connections')"
                                                                    type="line" 
                                                                    :options="{
                                                                        chart: {
                                                                            id: pod.name + '-connections',
                                                                            height: '45px',
                                                                            sparkline: {
                                                                                enabled: true
                                                                            }
                                                                        },
                                                                        stroke: {
                                                                            width: 2
                                                                        },
                                                                        tooltip: {
                                                                            enabled: false
                                                                        },
                                                                        colors: (pod.role == 'primary') ? ['#36A8FF'] : ['#5db4be']
                                                                    }"
                                                                    :series="[{
                                                                        name: pod.name + '-connections',
                                                                        data: podConnections[pod.name]
                                                                    }]">
                                                                </apexchart>
                                                            </div>
                                                        </div>
                                                    </div>
                                                </template>
                                                <template v-else>
                                                </template>
                                            </template>
                                            <template v-else-if="(status.type == 'Running')"> <!-- Restarted Pod -->
                                                <div class="pod restarting">
                                                    <div class="pod" :class="pod.role">
                                                        <div class="podInfo xsPad">
                                                            <strong class="podName">
                                                                {{ initialPod }}
                                                            </strong>
                                                        </div>
                                                        <div class="podStatus">
                                                            <div class="repStats flex flex-50 xsPad">
                                                                <div class="nodeIcon">
                                                                    <svg xmlns="http://www.w3.org/2000/svg" width="38.311" height="43.788" viewBox="0 0 38.311 43.788">
                                                                        <g transform="translate(-129.63 -69.152)">
                                                                            <g class="percent-75-100" transform="translate(0 0)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 84.57740783691406 C 143.7403259277344 84.57740783691406 139.0069885253906 83.9876708984375 135.4573059082031 82.91683197021484 C 132.3255157470703 81.97205352783203 130.3799896240234 80.68137359619141 130.3799896240234 79.54847717285156 L 130.3799896240234 74.930908203125 C 130.3799896240234 73.7979736328125 132.3253326416016 72.50729370117188 135.4568634033203 71.56256103515625 C 139.0063629150391 70.49172210693359 143.7398681640625 69.90199279785156 148.7854461669922 69.90199279785156 C 153.8305816650391 69.90199279785156 158.5639343261719 70.49172973632812 162.1136169433594 71.56256103515625 C 165.2454071044922 72.50733947753906 167.19091796875 73.79801177978516 167.19091796875 74.930908203125 L 167.19091796875 79.54847717285156 C 167.19091796875 80.68137359619141 165.2454071044922 81.97205352783203 162.1136169433594 82.91683197021484 C 158.5639343261719 83.9876708984375 153.83056640625 84.57740783691406 148.7854461669922 84.57740783691406 Z" stroke="none"/>
                                                                                <path class="out" d="M 148.7854461669922 70.65199279785156 C 143.8112487792969 70.65199279785156 139.1546630859375 71.23036956787109 135.6734771728516 72.28059387207031 C 131.9647064208984 73.39948272705078 131.1299896240234 74.59893798828125 131.1299896240234 74.930908203125 L 131.1299896240234 79.54847717285156 C 131.1299896240234 79.88043212890625 131.9647827148438 81.07984161376953 135.6739196777344 82.19879150390625 C 139.1552734375 83.2490234375 143.8116912841797 83.82740783691406 148.7854461669922 83.82740783691406 C 153.7592163085938 83.82740783691406 158.4156494140625 83.2490234375 161.8969879150391 82.19879150390625 C 165.6061248779297 81.07984924316406 166.44091796875 79.88043212890625 166.44091796875 79.54847717285156 L 166.44091796875 74.930908203125 C 166.44091796875 74.59896087646484 165.6061248779297 73.39954376220703 161.8969879150391 72.28060150146484 C 158.4156494140625 71.23036956787109 153.7592163085938 70.65199279785156 148.7854461669922 70.65199279785156 M 148.7854461669922 69.15199279785156 C 159.3642120361328 69.15199279785156 167.94091796875 71.73909759521484 167.94091796875 74.930908203125 L 167.94091796875 79.54847717285156 C 167.94091796875 82.74028015136719 159.3642120361328 85.32740783691406 148.7854461669922 85.32740783691406 C 138.2067260742188 85.32740783691406 129.6299896240234 82.74028015136719 129.6299896240234 79.54847717285156 L 129.6299896240234 74.930908203125 C 129.6299896240234 71.73909759521484 138.2055053710938 69.15199279785156 148.7854461669922 69.15199279785156 Z" stroke="none"/>
                                                                            </g>
                                                                            <g class="percent-50-75" transform="translate(0 2.919)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 90.86602783203125 C 143.7403259277344 90.86602783203125 139.0069885253906 90.27628326416016 135.4573059082031 89.2054443359375 C 132.3255157470703 88.26066589355469 130.3799896240234 86.96998596191406 130.3799896240234 85.83709716796875 L 130.3799896240234 81.5208740234375 C 131.9980316162109 82.82033538818359 134.6211547851562 83.65998840332031 136.6966552734375 84.15798187255859 C 140.1705322265625 84.99150848388672 144.3507690429688 85.43207550048828 148.7854461669922 85.43207550048828 C 153.2197265625 85.43207550048828 157.400146484375 84.99167633056641 160.874755859375 84.15848541259766 C 162.9495697021484 83.66095733642578 165.5720977783203 82.82216644287109 167.19091796875 81.52445983886719 L 167.19091796875 85.83709716796875 C 167.19091796875 86.96998596191406 165.2454071044922 88.26066589355469 162.1136169433594 89.2054443359375 C 158.5639343261719 90.27628326416016 153.83056640625 90.86602783203125 148.7854461669922 90.86602783203125 Z" stroke="none"/>
                                                                                <path class="out" d="M 166.44091796875 82.91915893554688 C 164.7964172363281 83.84923553466797 162.7451477050781 84.48123168945312 161.0496520996094 84.88780212402344 C 157.5186157226562 85.73452758789062 153.2777252197266 86.18207550048828 148.7854461669922 86.18207550048828 C 144.292724609375 86.18207550048828 140.0519866943359 85.73434448242188 136.5216674804688 84.88728332519531 C 134.2853240966797 84.35069274902344 132.4831848144531 83.69454956054688 131.1299896240234 82.92610168457031 L 131.1299896240234 85.83709716796875 C 131.1299896240234 86.16904449462891 131.9647827148438 87.36845397949219 135.6739196777344 88.48740386962891 C 139.1552734375 89.53763580322266 143.8116912841797 90.11602783203125 148.7854461669922 90.11602783203125 C 153.7592163085938 90.11602783203125 158.4156494140625 89.53763580322266 161.8969879150391 88.48740386962891 C 165.6061248779297 87.36846160888672 166.44091796875 86.16904449462891 166.44091796875 85.83709716796875 L 166.44091796875 82.91915893554688 M 129.9355621337891 80.16232299804688 C 130.0228881835938 80.16232299804688 130.1120910644531 80.19865417480469 130.1776885986328 80.27956390380859 C 132.224365234375 82.80720520019531 139.7789916992188 84.68207550048828 148.7854461669922 84.68207550048828 C 157.7931365966797 84.68207550048828 165.3465576171875 82.80720520019531 167.3944702148438 80.28335571289062 C 167.4601287841797 80.20194244384766 167.5491943359375 80.16549682617188 167.6363677978516 80.16550445556641 C 167.7916564941406 80.16551971435547 167.94091796875 80.28116607666016 167.94091796875 80.46427154541016 L 167.94091796875 85.83709716796875 C 167.94091796875 89.02889251708984 159.3642120361328 91.61602783203125 148.7854461669922 91.61602783203125 C 138.2067260742188 91.61602783203125 129.6299896240234 89.02889251708984 129.6299896240234 85.83709716796875 L 129.6299896240234 80.46174621582031 C 129.6299896240234 80.27775573730469 129.7798461914062 80.16232299804688 129.9355621337891 80.16232299804688 Z" stroke="none"/>
                                                                            </g>
                                                                            <g class="percent-25-50" transform="translate(0 4.847)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 98.13986206054688 C 143.7405242919922 98.13986206054688 139.0071563720703 97.55018615722656 135.457275390625 96.47944641113281 C 132.3254852294922 95.53481292724609 130.3799896240234 94.24456787109375 130.3799896240234 93.11220550537109 L 130.3799896240234 88.79533386230469 C 131.9990081787109 90.09484100341797 134.6217041015625 90.93424987792969 136.6966857910156 91.43201446533203 C 140.1708068847656 92.26542663574219 144.3510284423828 92.7059326171875 148.7854461669922 92.7059326171875 C 153.2197265625 92.7059326171875 157.400146484375 92.26553344726562 160.874755859375 91.43234252929688 C 162.9495697021484 90.93482208251953 165.5720977783203 90.09603118896484 167.19091796875 88.79833221435547 L 167.19091796875 93.11220550537109 C 167.19091796875 94.24456787109375 165.2454223632812 95.53481292724609 162.1136474609375 96.47944641113281 C 158.5637664794922 97.55018615722656 153.8303833007812 98.13986206054688 148.7854461669922 98.13986206054688 Z" stroke="none"/>
                                                                                <path class="out" d="M 131.1299896240234 90.19075775146484 L 131.1299896240234 93.11220550537109 C 131.1299896240234 93.44392395019531 131.9647674560547 94.64263153076172 135.6738586425781 95.76140594482422 C 139.1554260253906 96.81153106689453 143.8118743896484 97.38986206054688 148.7854461669922 97.38986206054688 C 153.759033203125 97.38986206054688 158.4154968261719 96.81153106689453 161.8970489501953 95.76140594482422 C 165.6061401367188 94.64264678955078 166.44091796875 93.44392395019531 166.44091796875 93.11220550537109 L 166.44091796875 90.19303131103516 C 164.7964172363281 91.12310791015625 162.7451477050781 91.75510406494141 161.0496520996094 92.16167449951172 C 157.5186309814453 93.00838470458984 153.2777252197266 93.4559326171875 148.7854461669922 93.4559326171875 C 144.2929992675781 93.4559326171875 140.0522766113281 93.00826263427734 136.5217437744141 92.16132354736328 C 134.8262176513672 91.75458526611328 132.7748870849609 91.12217712402344 131.1299896240234 90.19075775146484 M 129.9354248046875 87.43683624267578 C 130.0227966308594 87.43683624267578 130.112060546875 87.47329711914062 130.1776885986328 87.55471801757812 C 132.224365234375 90.08107757568359 139.7789916992188 91.9559326171875 148.7854461669922 91.9559326171875 C 157.7931365966797 91.9559326171875 165.3465576171875 90.08107757568359 167.3944702148438 87.55722808837891 C 167.4601440429688 87.47623443603516 167.5492553710938 87.43991851806641 167.6364593505859 87.43992614746094 C 167.7917175292969 87.43994140625 167.94091796875 87.55506134033203 167.94091796875 87.73814392089844 L 167.94091796875 93.11220550537109 C 167.94091796875 96.30276489257812 159.3642120361328 98.88986206054688 148.7854461669922 98.88986206054688 C 138.2067260742188 98.88986206054688 129.6299896240234 96.30276489257812 129.6299896240234 93.11220550537109 L 129.6299896240234 87.735595703125 C 129.6299896240234 87.55247497558594 129.7797546386719 87.43683624267578 129.9354248046875 87.43683624267578 Z" stroke="none"/>
                                                                            </g>
                                                                            <g class="percent-0-25" transform="translate(0 13.847)" stroke-miterlimit="10">
                                                                                <path class="in" d="M 148.7854461669922 98.13986206054688 C 143.7405242919922 98.13986206054688 139.0071563720703 97.55018615722656 135.457275390625 96.47944641113281 C 132.3254852294922 95.53481292724609 130.3799896240234 94.24456787109375 130.3799896240234 93.11220550537109 L 130.3799896240234 88.79533386230469 C 131.9990081787109 90.09484100341797 134.6217041015625 90.93424987792969 136.6966857910156 91.43201446533203 C 140.1708068847656 92.26542663574219 144.3510284423828 92.7059326171875 148.7854461669922 92.7059326171875 C 153.2197265625 92.7059326171875 157.400146484375 92.26553344726562 160.874755859375 91.43234252929688 C 162.9495697021484 90.93482208251953 165.5720977783203 90.09603118896484 167.19091796875 88.79833221435547 L 167.19091796875 93.11220550537109 C 167.19091796875 94.24456787109375 165.2454223632812 95.53481292724609 162.1136474609375 96.47944641113281 C 158.5637664794922 97.55018615722656 153.8303833007812 98.13986206054688 148.7854461669922 98.13986206054688 Z" stroke="none"/>
                                                                                <path class="out" d="M 131.1299896240234 90.19075775146484 L 131.1299896240234 93.11220550537109 C 131.1299896240234 93.44392395019531 131.9647674560547 94.64263153076172 135.6738586425781 95.76140594482422 C 139.1554260253906 96.81153106689453 143.8118743896484 97.38986206054688 148.7854461669922 97.38986206054688 C 153.759033203125 97.38986206054688 158.4154968261719 96.81153106689453 161.8970489501953 95.76140594482422 C 165.6061401367188 94.64264678955078 166.44091796875 93.44392395019531 166.44091796875 93.11220550537109 L 166.44091796875 90.19303131103516 C 164.7964172363281 91.12310791015625 162.7451477050781 91.75510406494141 161.0496520996094 92.16167449951172 C 157.5186309814453 93.00838470458984 153.2777252197266 93.4559326171875 148.7854461669922 93.4559326171875 C 144.2929992675781 93.4559326171875 140.0522766113281 93.00826263427734 136.5217437744141 92.16132354736328 C 134.8262176513672 91.75458526611328 132.7748870849609 91.12217712402344 131.1299896240234 90.19075775146484 M 129.9354248046875 87.43683624267578 C 130.0227966308594 87.43683624267578 130.112060546875 87.47329711914062 130.1776885986328 87.55471801757812 C 132.224365234375 90.08107757568359 139.7789916992188 91.9559326171875 148.7854461669922 91.9559326171875 C 157.7931365966797 91.9559326171875 165.3465576171875 90.08107757568359 167.3944702148438 87.55722808837891 C 167.4601440429688 87.47623443603516 167.5492553710938 87.43991851806641 167.6364593505859 87.43992614746094 C 167.7917175292969 87.43994140625 167.94091796875 87.55506134033203 167.94091796875 87.73814392089844 L 167.94091796875 93.11220550537109 C 167.94091796875 96.30276489257812 159.3642120361328 98.88986206054688 148.7854461669922 98.88986206054688 C 138.2067260742188 98.88986206054688 129.6299896240234 96.30276489257812 129.6299896240234 93.11220550537109 L 129.6299896240234 87.735595703125 C 129.6299896240234 87.55247497558594 129.7797546386719 87.43683624267578 129.9354248046875 87.43683624267578 Z" stroke="none"/>
                                                                            </g>                                                                                                                        
                                                                        </g>
                                                                    </svg>
                                                                </div>
                                                            </div>
                                                            <div class="podFooter xsPad">
                                                                <span class="connCount">
                                                                    <strong>0</strong></br>
                                                                    Connections
                                                                </span>
                                                                <span class="label status floatRight">
                                                                    <span>Restarting</span>
                                                                </span>
                                                            </div>
                                                            <div class="connGraph chart-wrapper" title="Real Time Connections"></div>
                                                        </div>
                                                    </div>
                                                </div>
                                            </template>
                                        </template>
                                    </div>
                                    <template v-else>
                                        <div class="no-data">
                                            No pods status available yet
                                        </div>
                                    </template>
                                </template>
                            </template>
                        </div>
                    </template>

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
                                    Run At
                                    <span class="helpTooltip" :data-tooltip="(timezone == 'local') ? getTooltip('sgdbops.spec.runAt').replace('UTC ','') : getTooltip('sgdbops.spec.runAt')"></span>
                                </td>
                                <td colspan="2" class="timestamp">
                                    <template v-if="op.data.spec.hasOwnProperty('runAt')">
                                        <span class='date'>
                                            {{ op.data.spec.runAt | formatTimestamp('date') }}
                                        </span>
                                        <span class='time'>
                                            {{ op.data.spec.runAt | formatTimestamp('time') }}
                                        </span>
                                        <span class='tzOffset'>{{ showTzOffset() }}</span>
                                    </template>
                                    <template v-else>
                                        <span>ASAP</span>
                                    </template>
                                </td>
                            </tr>
                            <tr>
                                <td class="label">
                                    Max Retries
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.maxRetries')"></span>
                                </td>
                                <td colspan="2" class="textRight">
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
                                    <td class="textRight">
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
                                        {{ isEnabled(op.data.spec.benchmark.pgbench.usePreparedStatements) }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Concurrent Clients
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.concurrentClients')"></span>
                                    </td>
                                    <td class="textRight">
                                        {{ op.data.spec.benchmark.pgbench.hasOwnProperty('concurrentClients') ? op.data.spec.benchmark.pgbench.concurrentClients : '1' }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Threads
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.threads')"></span>
                                    </td>
                                    <td colspan="2" class="textRight">
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
                                        {{ isEnabled(op.data.spec.majorVersionUpgrade.link) }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Clone
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.clone')"></span>
                                    </td>
                                    <td>
                                        {{ isEnabled(op.data.spec.majorVersionUpgrade.clone) }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Check
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.check')"></span>
                                    </td>
                                    <td>
                                        {{ isEnabled(op.data.spec.majorVersionUpgrade.check) }}
                                    </td>
                                </tr>
                                <tr v-if="hasProp(op, 'data.spec.majorVersionUpgrade.backupPath') && op.data.spec.majorVersionUpgrade.backupPath.length">
                                    <td class="label">
                                        Backup Path
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.backupPath')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.majorVersionUpgrade.backupPath }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Target Postgres Version
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.postgresVersion')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.majorVersionUpgrade.postgresVersion }}
                                    </td>
                                </tr>
                                <tr>
                                    <td class="label">
                                        Target Postgres Configuration
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.sgPostgresConfig')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.majorVersionUpgrade.sgPostgresConfig }}
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
                                <tr>
                                    <td class="label">
                                        Target Postgres Version
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.postgresVersion')"></span>
                                    </td>
                                    <td>
                                        {{ op.data.spec.minorVersionUpgrade.postgresVersion }}
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
                                            {{ isEnabled(op.data.spec.repack.noOrder) }}
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
                                            {{ isEnabled(op.data.spec.repack.noKillBackend) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(op.data.spec.repack.noAnalyze) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Exclude Extension
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(op.data.spec.repack.excludeExtension) }}
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
                                 <tr v-if="op.data.spec.restart.hasOwnProperty('onlyPendingRestart')">
                                    <td class="label">
                                        Restart Pending Pods Only
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart.onlyPendingRestart')"></span> 
                                    </td>
                                    <td>
                                        {{ op.data.spec.restart.onlyPendingRestart }}
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
                                            {{ isEnabled(op.data.spec.vacuum.full) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Freeze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(op.data.spec.vacuum.freeze) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(op.data.spec.vacuum.analyze) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Page Skipping
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping').replace('Defaults to: `false`', 'Enabled by default')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(op.data.spec.vacuum.disablePageSkipping, true) }}
                                        </td>
                                    </tr>
                                </tbody>
                        </template>
                    </table> 

                    <template v-if="op.data.spec.op === 'repack' && op.data.spec.hasOwnProperty('repack') && op.data.spec.repack.hasOwnProperty('databases') && op.data.spec.repack.databases.length">
                        <h2 class="capitalize">
                            {{ op.data.spec.op }} Databases
                        </h2>
                        <table class="crdDetails">
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
                                            {{ isEnabled(db.noOrder) }}
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
                                            {{ isEnabled(db.noKillBackend) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            No Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze')"></span>
                                        </td>
                                        <td>
                                            {{ isEnabled(db.noAnalyze) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Exclude Extension
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span>
                                        </td>
                                        <td>
                                            {{ isEnabled(db.excludeExtension) }}                                            
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
                        <table class="crdDetails">
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
                                            {{ isEnabled(db.full) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Freeze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(db.freeze) }}
                                        </td>
                                    </tr>
                                    <tr>
                                        <td class="label">
                                            Analyze
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(db.analyze) }}
                                        </td>
                                    </tr>
                                    <tr v-if="db.hasOwnProperty('disablePageSkipping')">
                                        <td class="label">
                                            Page Skipping
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping').replace('Defaults to: `false`', 'Enabled by default')"></span> 
                                        </td>
                                        <td>
                                            {{ isEnabled(db.disablePageSkipping, true) }}
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
                                    <td class="timestamp" colspan="3">
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
                                    <td colspan="3" class="textRight">
                                        {{ op.data.status.opRetries }}
                                    </td>
                                </tr>

                                <template v-if="op.data.spec.op === 'benchmark' && op.data.status.hasOwnProperty('benchmark')">
                                    <tr v-if="op.data.status.benchmark.hasOwnProperty('pgbench')">
                                        <td class="label" :rowspan="Object.keys(op.data.status.benchmark.pgbench).length + 3">
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
                                        <td colspan="2" class="textRight">
                                            {{ op.data.status.benchmark.pgbench.scaleFactor }}
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status.benchmark.pgbench.hasOwnProperty('transactionsProcessed')">
                                        <td class="label">
                                            Transactions Processed
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsProcessed')"></span>
                                        </td>
                                        <td colspan="2" class="textRight">
                                            {{ op.data.status.benchmark.pgbench.transactionsProcessed }}
                                        </td>
                                    </tr>
                                    <template v-if="( hasProp(op, 'data.status.benchmark.pgbench.latency.average.value') && hasProp(op, 'data.status.benchmark.pgbench.latency.standardDeviation.value') )">
                                        <tr>
                                            <td class="label" rowspan="2">
                                                Latency
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latency')"></span>
                                            </td>
                                            <td class="label">
                                                Average
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latency.average')"></span>
                                            </td>
                                            <td class="textRight">
                                                {{ op.data.status.benchmark.pgbench.latency.average.value }} {{ op.data.status.benchmark.pgbench.latency.average.unit }}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">
                                                Standard Deviation
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latency.standardDeviation')"></span>
                                            </td>
                                            <td class="textRight">
                                                {{ op.data.status.benchmark.pgbench.latency.standardDeviation.value }} {{ op.data.status.benchmark.pgbench.latency.standardDeviation.unit }}
                                            </td>
                                        </tr>
                                    </template>
                                    <template v-if="( hasProp(op, 'data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.value') && hasProp(op, 'data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.value') )">
                                        <tr>
                                            <td class="label" rowspan="2">
                                                Transactions per Second
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsPerSecond')"></span>
                                            </td>
                                            <td class="label">
                                                Including Connections Establishing
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing')"></span>
                                            </td>
                                            <td class="textRight">
                                                {{ op.data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.value }} {{ op.data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.unit }}
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="label">
                                                Excluding Connections Establishing
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing')"></span>
                                            </td>
                                            <td class="textRight">
                                                {{ op.data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.value }} {{ op.data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.unit }}
                                            </td>
                                        </tr>
                                    </template>
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
                                        <td class="timestamp">
                                             <span class='date'>
                                                {{ op.data.status[op.data.spec.op].switchoverInitiated | formatTimestamp('date') }}
                                            </span>
                                            <span class='time'>
                                                {{ op.data.status[op.data.spec.op].switchoverInitiated | formatTimestamp('time') }}
                                            </span>
                                            <span class='tzOffset'>{{ showTzOffset() }}</span>
                                        </td>
                                    </tr>
                                    <tr v-if="op.data.status[op.data.spec.op].hasOwnProperty('switchoverFinalized')">
                                        <td class="label">
                                            Switchover Finalized
                                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + op.data.spec.op + '.switchoverFinalized')"></span>
                                        </td>
                                        <td class="timestamp">
                                             <span class='date'>
                                                {{ op.data.status[op.data.spec.op].switchoverFinalized | formatTimestamp('date') }}
                                            </span>
                                            <span class='time'>
                                                {{ op.data.status[op.data.spec.op].switchoverFinalized | formatTimestamp('time') }}
                                            </span>
                                            <span class='tzOffset'>{{ showTzOffset() }}</span>
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

                        <h2>Operation Events</h2>
                        <div class="fixedHeight">
                            <table class="events resizable" v-columns-resizable>
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
                                    <th class="involvedObject hasTooltip" data-type="involvedObject">
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
                                                No recent events have been recorded for this operation.
                                            </td>
                                        </tr>
                                    </template>
                                    <template v-else>
                                        <template v-for="event in events">
                                            <tr class="base">
                                                <td class="timestamp hasTooltip">
                                                    <span v-if="event.hasOwnProperty('firstTimestamp')">
                                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
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
                                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
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
                                                <td class="involvedObject hasTooltip">
                                                    <span>
                                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
                                                            {{ event.involvedObject.kind }}/{{ event.involvedObject.name }}
                                                        </router-link>
                                                    </span>
                                                </td>
                                                <td class="eventMessage hasTooltip">
                                                    <span>
                                                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + $route.params.name + '/event/' + event.metadata.uid" class="noColor">
                                                            {{ event.message }}
                                                        </router-link>
                                                    </span>
                                                </td>
                                            </tr>
                                        </template>
                                    </template>
                                </tbody>
                            </table>
                        </div>
                    
                        <div id="nameTooltip">
                            <div class="info"></div>
                        </div>
                    </template>
                </template>
            </template>
            
            <template v-else-if="$route.params.hasOwnProperty('name') && $route.params.hasOwnProperty('uid')">
                <div class="relative">
                    <h2>Event Details</h2>
                    <div class="titleLinks">
                        <router-link :to="'/' + $route.params.namespace + '/sgdbop/' + $route.params.name" title="Close Event Details">Close Event Details</router-link>
                    </div>
                    <div class="configurationDetails" v-for="event in events" v-if="event.metadata.uid == $route.params.uid">
                        <table class="events crdDetails">
                            <tbody>
                                <tr>
                                    <td class="label">Name</td>
                                    <td>{{ event.metadata.name }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('type') && event.type">
                                    <td class="label">Type</td>
                                    <td>{{ event.type }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('eventTime') && event.eventTime">
                                    <td class="label">Event Time</td>
                                    <td class="timestamp">
                                        <span class='date'>
                                            {{ event.eventTime | formatTimestamp('date') }}
                                        </span>
                                        <span class='time'>
                                            {{ event.eventTime | formatTimestamp('time') }}
                                        </span>
                                        <span class='ms'>
                                            {{ event.eventTime | formatTimestamp('ms') }}
                                        </span>
                                        <span class='tzOffset'>{{ showTzOffset() }}</span>
                                    </td>
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
                                <tr v-if="event.hasOwnProperty('message') && event.message">
                                    <td class="label">Message</td>
                                    <td>{{ event.message }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('action') && event.action">
                                    <td class="label">Action</td>
                                    <td>{{ event.action }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('count') && event.count">
                                    <td class="label">Count</td>
                                    <td>{{ event.count }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('reason') && event.reason">
                                    <td class="label">Reason</td>
                                    <td>{{ event.reason }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('involvedObject') && event.involvedObject">
                                    <td class="label">Involved Object</td>
                                    <td>{{ event.involvedObject.kind }}/{{ event.involvedObject.name }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('related') && event.related">
                                    <td class="label">Related</td>
                                    <td>{{ event.related.kind }}/{{ event.related.name }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('reportingComponent') && event.reportingComponent">
                                    <td class="label">Reporting Component</td>
                                    <td class="vPad">{{ event.reportingComponent }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('reportingInstance') && event.reportingInstance">
                                    <td class="label">Reporting Instance</td>
                                    <td class="vPad">{{ event.reportingInstance }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('series') && event.series && event.series.hasOwnProperty('count') && event.series.count">
                                    <td class="label">Series Count</td>
                                    <td>{{ event.series.count }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('series') && event.series && event.series.hasOwnProperty('lastObservedTime') && event.series.lastObservedTime">
                                    <td class="label">Series Last Observed Time</td>
                                    <td class="timestamp">
                                        <span class='date'>
                                            {{ event.series.lastObservedTime | formatTimestamp('date') }}
                                        </span>
                                        <span class='time'>
                                            {{ event.series.lastObservedTime | formatTimestamp('time') }}
                                        </span>
                                        <span class='ms'>
                                            {{ event.series.lastObservedTime | formatTimestamp('ms') }}
                                        </span>
                                        <span class='tzOffset'>{{ showTzOffset() }}</span>
                                    </td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('series') && event.series && event.series.hasOwnProperty('state') && event.series.state">
                                    <td class="label">Series State</td>
                                    <td>{{ event.series.state }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('source') && event.source && event.source.hasOwnProperty('component') && event.source.component">
                                    <td class="label">Source Component</td>
                                    <td>{{ event.source.component }}</td>
                                </tr>
                                <tr v-if="event.hasOwnProperty('source') && event.source && event.source.hasOwnProperty('host') && event.source.host">
                                    <td class="label">Source Host</td>
                                    <td>{{ event.source.host }}</td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
    import { mixin } from './mixins/mixin'
    import store from '../store'
    import moment from 'moment'
    import sgApi from '../api/sgApi'

    export default {
        name: 'SGDbOps',

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
				},
                podConnections: {},
                pollClusterStats: 0,
                events: [],
                eventsPooling: null
            }
        },

        mounted: function() {
			const vc = this;

			vc.getOpEvents();
			vc.eventsPooling = setInterval( function() {
				vc.getOpEvents()
			}, 10000);
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

            getElapsedTime(op) {

                if( op.data.hasOwnProperty('status') ) {
                    let lastStatus = op.data.status.conditions.find(c => (c.status === 'True') )
                    let begin = moment(op.data.status.opStarted)
                    let finish = (lastStatus.type == 'Running') ? moment() : moment(lastStatus.lastTransitionTime);
                    let elapsed = moment.duration(finish.diff(begin));
                    return elapsed.toString().substring(2).replace('H','h ').replace('M','m ').replace(/\..*S/,'s').replace('T','')
                } else {
                    return '-'
                }   
            },

            hasTimedOut(op) {
                if( op.data.hasOwnProperty('status') ) {
                    let failedOp = op.data.status.conditions.find(c => (c.status === 'True') && (c.type == 'Failed') && (c.reason == 'OperationTimedOut' ) )

                    if( typeof failedOp !== 'undefined' )
                        return 'YES'
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

            getPodConnections(podName, connections) {
                const vc = this;

                if(vc.podConnections.hasOwnProperty(podName)) {
                    vc.podConnections[podName].push(connections);
                } else {
                    vc.podConnections[podName] = [];
                }
                
                return connections
            },

            getOpEvents() {
				const vc = this;
				
				sgApi
				.getResourceDetails('sgdbops', vc.$route.params.namespace, vc.$route.params.name, 'events')
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
				}).catch(function(err) {
					console.log(err);
					vc.checkAuthError(err);
				});
			},
        },
        
        computed: {

            dbOps () {
                const vc = this
				
				store.state.sgdbops.forEach( function(op, index) {

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

                // If it's a minorVersionUpgrade with running status, update cluster stats every 2sec
                if( (vc.pollClusterStats == 0) && vc.$route.params.hasOwnProperty('name') ) {
                    
                    let op = store.state.sgdbops.find(op => ( (op.data.metadata.name == vc.$route.params.name) && (op.data.metadata.namespace == vc.$route.params.namespace) ))
                    
                    if( (typeof op != 'undefined') && (op.data.spec.op == 'minorVersionUpgrade') && vc.hasProp(op, 'data.status.conditions') ) {
                        let status = op.data.status.conditions.find( c => ( (['Running', 'Completed'].includes(c.type)) && (c.status == 'True') ) )

                        if(typeof status != 'undefined') {

                            vc.pollClusterStats = setInterval( function() {
                                sgApi
                                .getResourceDetails('sgclusters', vc.$route.params.namespace, op.data.spec.sgCluster, 'stats')
                                .then( function(resp) {
                                    store.commit('updateClusterStats', {
                                        name: op.data.spec.sgCluster,
                                        namespace: vc.$route.params.namespace,
                                        stats: resp.data
                                    })
                                }).catch(function(err) {
                                    console.log(err);
                                });
                            }, 5000)

                        }
                    }
                }
                
				return vc.sortTable( [...(store.state.sgdbops.filter(op => ( op.show && ( op.data.metadata.namespace == vc.$route.params.namespace ))))], vc.currentSort.param, vc.currentSortDir, vc.currentSort.type)
            },
            
            tooltips() {
                return store.state.tooltips
            },

            clusters() {
                return store.state.sgclusters
            },

            isFiltered() {
                return ( this.filters.clusterName.length || this.filters.op.length || this.filters.status.length)
            },

            timezone () {
                return store.state.timezone
            },

            primaryNodeDisk() {
                const vc = this;

                let op = store.state.sgdbops.find(o => (o.data.metadata.name == vc.$route.params.name) && (o.data.metadata.namespace == vc.$route.params.namespace) );
                let cluster = store.state.sgclusters.find(c => (c.data.metadata.name == op.data.spec.sgCluster) && (op.data.metadata.namespace == vc.$route.params.namespace));
                let primaryNode = cluster.status.pods.find(p => (p.role == 'primary') );

                return ( (typeof primaryNode != 'undefined') && primaryNode.hasOwnProperty('diskUsed') ) ? vc.getBytes(primaryNode.diskUsed) : null

            }
        },

        beforeDestroy: function() {
            clearInterval(this.pollClusterStats);
            clearInterval(this.eventsPooling)
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

    .clusterStatus {
        border: 1px solid var(--borderColor);
        margin: 5px 0 10px;
        padding: 10px 15px;
        display: inline-block;
        width: 100%;
    }

    .flex {
        display: flex;
    }

    .flex.flex-50 > * {
        flex: 50%;
    }

    .flex.flex-33 > * {
        flex: 33.33%;
    }

    .darkmode .clusterStatus {
        border-color: #444;
    }

    .repStats .nodeIcon path.in {
        opacity: .25;
    }

    .repStats .nodeIcon .full path.in, .primary .repStats .nodeIcon path.in {
        opacity: 1;
    }

    .repStats .nodeIcon path {
        fill: #5db4be;
    }

    .primary .repStats .nodeIcon path {
        fill: var(--baseColor);
    }

    .clusterStatus .pod {
        width: 33.33%;
        display: inline-block;
        margin-bottom: 40px;
        padding: 0 10px;
    }

    .pod > .podStatus {
        border: 2px solid #5db4be;
        border-radius: 5px;
        overflow: hidden;
        width: 100%;
        position: relative;
        padding-bottom: 50px;
    }

    .pod.primary > .podStatus {
        border-color: var(--baseColor);
    }

    .pod.primary .label.primary span {
        color: var(--baseColor);
        background: rgb(54 168 255 / 20%);
    }

    .pod.Pending {
        filter: grayscale(1);
    }
    
    .connCount strong, .podName {
        color: #5db4be;
    }

    .primary .connCount strong, .primary .podName {
        color: var(--baseColor);
    }

    .podInfo .podStatus {
        position: relative;
        top: -2px;
    }

    .podInfo .podStatus .label span {
        color: #5db4be;
        background: rgb(93 180 190 / 20%);
    }

    .pgVersion .label span {
        width: auto;
        color: #5db4be; 
    }

    .primary .pgVersion .label span {
        color: var(--baseColor);
        border-color: var(--baseColor);
    }

    .podFooter, .connGraph {
        background: var(--inputBg);
        height: 50px;
    }

    .podFooter .label.status {
        position: absolute;
        bottom: 12px;
        right: 10px;
    }

    .podFooter {
        position: absolute;
        width: 100%;
        bottom: 0;
    }

    .connGraph {
        padding: 5px;
    }

    .connGraph .apexcharts-canvas, .connGraph .apexcharts-canvas > svg {
        max-height: 45px;
        overflow: hidden;
    }

    h3.header {
        border-bottom: 1px solid var(--borderColor);
        margin: 10px;
    }

    .darkmode h3.header {
        border-color: #444;
    }

    .label span {
        border-radius: 25px;
        padding: 1px 10px;
        font-weight: bold;
        font-size: 85%;
    }

    .pgVersion .label span {
        border: 2px solid #5db4be;
    }

    .pgVersion .label {
        margin: 5px 0;
        display: inline-block;
    }

    .dataPercent strong {
        font-size: 120%;
        color: #5db4be;
    }

    .dataPercent {
        display: inline-block;
        padding-left: 10px;
        position: relative;
        top: -7px;
    }

    .stopWatch {
        color: var(--baseColor);
        position: absolute;
        right: 10px;
    }

    .stopWatch span:nth-child(n+3):before {
        content: ":";
    }

    .stopWatch svg {
        width: 16px;
        position: relative;
        top: 2px;
        margin-right: 5px;
    }

    .upgradeLog table {
        width: calc(100% - 20px);
        margin: -11px 10px 20px;
        margin-top: -11px;
    }

    .upgradeLog td.timestamp {
        width: 200px;
        border-right: 1px solid var(--borderColor);
    }

    .upgradeLog + .header {
        margin-top: 25px;
    }

    td.phase a.failed {
        position: relative;
    }

    span.helpTooltip.failed, .darkmode span.helpTooltip.failed {
        position: absolute;
        width: 10px;
        height: 10px;
        left: -3px;
        top: -2px;
        background: red !important;
        border: 2px solid var(--rowBg);
    }

    tr:nth-child(even) span.helpTooltip.alert, .darkmode tr:nth-child(even) span.helpTooltip.alert {
        border-color: var(--activeBg);
    }

    .fixedHeight {
        max-height: 33vh;
        overflow-y: auto;
        overflow-x: hidden;
        margin-bottom: 20px;
    }

    table.resizable th[data-type="phase"] {
		max-width: 105px;
	}

    table.resizable th[data-type="type"] {
		max-width: 190px;
	}

    table.resizable th[data-type="timedOut"] {
		max-width: 120px;
	}

    table.resizable th[data-type="involvedObject"] {
		min-width: 150px;
		max-width: 200px;
	}

    @media screen and (min-width: 2600px) {
        .clusterStatus .pod {
            width: 20%;
        }
    }

    @media screen and (min-width: 1920px) {
        .clusterStatus .pod {
            width: 25%;
        }
    }

</style>