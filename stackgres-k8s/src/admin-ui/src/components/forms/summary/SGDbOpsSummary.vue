<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                    <li v-if="hasProp(crd, 'data.metadata.uid')">
                        <strong class="label">UID</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.metadata.uid')"></span>
                        <span class="value"> : {{ crd.data.metadata.uid }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Specs </strong>
                <ul>
                    <li>
                        <strong class="label">Operation</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.op')"></span>
                        <span class="value capitalize"> : {{ splitUppercase(crd.data.spec.op) }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Cluster</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.sgCluster')"></span>
                        <span class="value"> :
                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + crd.data.spec.sgCluster" target="_blank"> 
                                {{ crd.data.spec.sgCluster }}
                                <span class="eyeIcon"></span>
                            </router-link>
                        </span>
                    </li>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.runAt') )">
                        <strong class="label">Run At</strong>
                        <span class="helpTooltip" :data-tooltip="(timezone == 'local') ? getTooltip('sgdbops.spec.runAt').replace('UTC ','') : getTooltip('sgdbops.spec.runAt')"></span>
                        <span class="value timestamp" v-if="hasProp(crd, 'data.spec.runAt')"> : 
                            <span class='date'>
                                {{ crd.data.spec.runAt | formatTimestamp('date') }}
                            </span>
                            <span class='time'>
                                {{ crd.data.spec.runAt | formatTimestamp('time') }}
                            </span>
                            <span class='ms'>
                                {{ crd.data.spec.runAt | formatTimestamp('ms') }}
                            </span>
                            <span class='tzOffset'>{{ showTzOffset() }}</span>
                        </span>
                        <span v-else class="value">
                            ASAP
                        </span>
                    </li>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.timeout') )">
                        <strong class="label">Timeout</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.timeout')"></span>
                        <span class="value"> : {{ hasProp(crd, 'data.spec.timeout') ? getIsoDuration(crd.data.spec.timeout) : 'NONE' }}</span>
                    </li>
                    <li v-if="crd.data.spec.hasOwnProperty('timeout')">
                        <strong class="label">Timed Out</strong>
                        <span class="helpTooltip" data-tooltip="States whether the operation failed because of timeout expiration."></span>
                        <span class="value"> : {{ hasTimedOut(crd) }}</span>
                    </li>
                    <li v-if="( showDefaults || (crd.data.spec.maxRetries > 0) )">
                        <strong class="label">Max Retries</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.maxRetries')"></span>
                        <span class="value"> :  {{ crd.data.spec.hasOwnProperty('maxRetries') ? crd.data.spec.maxRetries : '1' }}</span>
                    </li>
                    <li v-if="hasProp(crd, 'data.spec.scheduling.tolerations')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Node Tolerations</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.scheduling.tolerations')"></span>
                        <ul>
                            <li v-for="(toleration, index) in crd.data.spec.scheduling.tolerations">
                                <button class="toggleSummary"></button>
                                <strong class="label">Toleration #{{ index+1 }}</strong>
                                <ul>
                                    <li>
                                        <strong class="label">Key</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.scheduling.tolerations.key')"></span>
                                        <span class="value"> : {{ toleration.key }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Operator</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.scheduling.tolerations.operator')"></span>
                                        <span class="value"> : {{ toleration.operator }}</span>
                                    </li>
                                    <li v-if="toleration.hasOwnProperty('value')">
                                        <strong class="label">Value</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.scheduling.tolerations.value')"></span>
                                        <span class="value"> : {{ toleration.value }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Effect</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.scheduling.tolerations.effect')"></span>
                                        <span class="value"> : {{ toleration.effect ? toleration.effect : 'MatchAll' }}</span>
                                    </li>
                                    <li v-if="( toleration.hasOwnProperty('tolerationSeconds') && (toleration.tolerationSeconds != null) )">
                                        <strong class="label">Toleration Seconds</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.scheduling.tolerations.tolerationSeconds')"></span>
                                        <span class="value"> : {{ toleration.tolerationSeconds }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( (crd.data.spec.op == 'restart') && ( showDefaults || ( (crd.data.spec.restart.method != 'InPlace') || crd.data.spec.restart.onlyPendingRestart ) ) )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Restart Operation Specs </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart')"></span> 
                <ul>
                    <li v-if="( showDefaults || (crd.data.spec.restart.method != 'InPlace') )">
                        <strong class="label">Method</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart.method')"></span> 
                        <span class="value"> : {{ splitUppercase(crd.data.spec.restart.method) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.restart.onlyPendingRestart )">
                        <strong class="label">Pods to Restart</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.restart.onlyPendingRestart')"></span> 
                        <span class="value"> : {{ crd.data.spec.restart.onlyPendingRestart ? 'Pending pods only' : 'All pods' }}</span>
                    </li>
                </ul>
            </li>
        </ul>
        
        <ul class="section" v-else-if="(crd.data.spec.op == 'securityUpgrade' && (showDefaults || (crd.data.spec.securityUpgrade.method != 'InPlace')))">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Security Upgrade Operation Specs </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.securityUpgrade')"></span> 
                <ul>
                    <li>
                        <strong class="label">Method</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.securityUpgrade.method')"></span> 
                        <span class="value"> : {{ splitUppercase(crd.data.spec.securityUpgrade.method) }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.op == 'minorVersionUpgrade')">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Minor Version Upgrade Operation Specs </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade')"></span> 
                <ul>
                    <li v-if="( showDefaults || (crd.data.spec.minorVersionUpgrade.method != 'InPlace') )">
                        <strong class="label">Method</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.method')"></span>
                        <span class="value"> : {{ splitUppercase(crd.data.spec.minorVersionUpgrade.method) }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Postgres Version</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.minorVersionUpgrade.postgresVersion')"></span>
                        <span class="value"> : {{ crd.data.spec.minorVersionUpgrade.postgresVersion }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.op == 'majorVersionUpgrade')">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Major Version Upgrade Operation Specs </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade')"></span>
                <ul>
                    <li v-if="( showDefaults || crd.data.spec.majorVersionUpgrade.link )">
                        <strong class="label">Hard Link Files</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.link')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.majorVersionUpgrade.link) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.majorVersionUpgrade.clone )">
                        <strong class="label">Clone Files</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.clone')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.majorVersionUpgrade.clone) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.majorVersionUpgrade.check )">
                        <strong class="label">Check Clusters</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.check')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.majorVersionUpgrade.check) }}</span>
                    </li>
                    <li v-if="hasProp(crd, 'data.spec.majorVersionUpgrade.backupPath') && !isNull(crd.data.spec.majorVersionUpgrade.backupPath)">
                        <strong class="label">Backup Path</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.backupPath')"></span>
                        <span class="value"> : {{ crd.data.spec.majorVersionUpgrade.backupPath }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Postgres Version</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.postgresVersion')"></span>
                        <span class="value"> : {{ crd.data.spec.majorVersionUpgrade.postgresVersion }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Postgres Configuration</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.majorVersionUpgrade.sgPostgresConfig')"></span>
                        <span class="value"> :
                            <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + crd.data.spec.majorVersionUpgrade.sgPostgresConfig" target="_blank"> 
                                {{ crd.data.spec.majorVersionUpgrade.sgPostgresConfig }}
                                <span class="eyeIcon"></span>
                            </router-link>
                        </span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( (crd.data.spec.op == 'vacuum') && ( showDefaults || (crd.data.spec.vacuum.full || crd.data.spec.vacuum.freeze || !crd.data.spec.vacuum.analyze || crd.data.spec.vacuum.disablePageSkipping || hasProp(crd, 'data.spec.vacuum.databases')) ) )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Vacuum Operation Specs </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum')"></span>
                <ul>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.full )">
                        <strong class="label">Full Vacuum</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.full')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.vacuum.full) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.freeze )">
                        <strong class="label">Freeze</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.vacuum.freeze) }}</span>
                    </li>
                    <li v-if="( showDefaults || !crd.data.spec.vacuum.analyze )">
                        <strong class="label">Analyze</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.vacuum.analyze) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.disablePageSkipping )">
                        <strong class="label">Page Skipping</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping').replace('Defaults to: `false`', 'Enabled by default')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.vacuum.disablePageSkipping, true) }}</span>
                    </li>
                    
                    <li v-if="hasProp(crd, 'data.spec.vacuum.databases')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Database Specific Specs</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.databases')"></span> 
                        <ul>
                            <li v-for="db in crd.data.spec.vacuum.databases">
                                <button class="toggleSummary"></button>
                                <strong class="label">Database</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.vacuum.name')"></span>
                                <span class="value"> : {{ db.name }}</span>
                                <ul>
                                    <li>
                                        <strong class="label">Full Vacuum</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.full')"></span> 
                                        <span class="value"> : {{ isEnabled(db.full) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Freeze</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.freeze')"></span> 
                                        <span class="value"> : {{ isEnabled(db.freeze) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Analyze</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.analyze')"></span> 
                                        <span class="value"> : {{ isEnabled(db.analyze) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Page Skipping</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.vacuum.disablePageSkipping').replace('Defaults to: `false`', 'Enabled by default')"></span> 
                                        <span class="value"> : {{ isEnabled(db.disablePageSkipping, true) }}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.op == 'benchmark')">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Benchmark Operation Specs </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark')"></span>
                <ul>
                    <li v-if="(showDefaults || crd.data.spec.benchmark.type != 'pgbench')">
                        <strong class="label">Type</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.type')"></span>
                        <span class="value"> : {{ crd.data.spec.benchmark.type }}</span>
                    </li>
                    <li v-if="( showDefaults || (crd.data.spec.benchmark.connectionType != 'primary-service') )">
                        <strong class="label">Connection Type</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.connectionType')"></span>
                        <span class="value"> : {{ (crd.data.spec.benchmark.connectionType == 'primary-service') ? 'Primary Service' : 'Replicas Service' }}</span>
                    </li>

                    <li v-if="(crd.data.spec.benchmark.type == 'pgbench')">
                        <button class="toggleSummary"></button>
                        <strong class="label">PgBench Specs</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench')"></span>
                        <ul>
                            <li>
                                <strong class="label">Database Size</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.databaseSize')"></span>
                                <span class="value"> : {{ crd.data.spec.benchmark.pgbench.databaseSize }}</span>
                            </li>
                            <li v-if="( showDefaults || crd.data.spec.benchmark.pgbench.usePreparedStatements )">
                                <strong class="label">Prepared Statements</strong>
                                 <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.usePreparedStatements')"></span>
                                <span class="value"> : {{ isEnabled(crd.data.spec.benchmark.pgbench.usePreparedStatements) }}</span>
                            </li>
                            <li v-if="( showDefaults || (crd.data.spec.benchmark.pgbench.concurrentClients > 1) )">
                                <strong class="label">Concurrent Clients</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.concurrentClients')"></span>
                                <span class="value"> : {{ crd.data.spec.benchmark.pgbench.concurrentClients }}</span>
                            </li>
                            <li v-if="( showDefaults || (crd.data.spec.benchmark.pgbench.threads > 1) )">
                                <strong class="label">Threads</strong>
                                 <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.threads')"></span>
                                <span class="value"> : {{ crd.data.spec.benchmark.pgbench.threads }}</span>
                            </li>
                            <li>
                                <strong class="label">Duration</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.benchmark.pgbench.duration')"></span>
                                <span class="value"> : {{ getIsoDuration(crd.data.spec.benchmark.pgbench.duration) }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( (crd.data.spec.op == 'repack') && ( showDefaults || (crd.data.spec.repack.noOrder || crd.data.spec.repack.waitTimeout || crd.data.spec.repack.noKillBackend || crd.data.spec.repack.noAnalyze || crd.data.spec.repack.excludeExtension || hasProp(crd, 'data.spec.repack.databases') ) ) )">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Repack Details </strong>
                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack')"></span>
                <ul>
                    <li v-if="( showDefaults || crd.data.spec.repack.noOrder )">
                        <strong class="label">Order</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noOrder').replace('If true','Disable to').replace('Defaults to: `false`','Enabled by default')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.repack.noOrder, true) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.repack.noKillBackend )">
                        <strong class="label">Kill Backend</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noKillBackend').replace('If true don\'t','Disable to not').replace('Defaults to: `false`','Enabled by default')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.repack.noKillBackend, true) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.repack.noAnalyze )">
                        <strong class="label">Analyze</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze').replace('If true don\'t','Disable to not').replace('Defaults to: `false`','Enabled by default')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.repack.noAnalyze, true) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.repack.excludeExtension )">
                        <strong class="label">Exclude Extension</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span> 
                        <span class="value"> : {{ isEnabled(crd.data.spec.repack.excludeExtension) }}</span>
                    </li>
                    <li v-if="(showDefaults || crd.data.spec.repack.waitTimeout)">
                        <strong class="label">Wait Timeout</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.waitTimeout')"></span>
                        <span class="value"> : {{ hasProp(crd, 'data.spec.repack.waitTimeout') ? getIsoDuration(crd.data.spec.repack.waitTimeout) : 'No Timeout'}}</span>
                    </li>
                    
                    <li v-if="hasProp(crd, 'data.spec.repack.databases')">
                        <button class="toggleSummary"></button>
                        <strong class="label">Database Specific Specs</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases')"></span>
                        <ul>
                            <li v-for="db in crd.data.spec.repack.databases">
                                <button class="toggleSummary"></button>
                                <strong class="label">Database</strong>
                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.databases.name')"></span>
                                <span class="value"> : {{ db.name }}</span>
                                <ul>
                                    <li>
                                        <strong class="label">Order</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noOrder').replace('If true','Disable to').replace('Defaults to: `false`','Enabled by default')"></span>
                                        <span class="value"> : {{ isEnabled(db.noOrder, true) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Kill Backend</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noKillBackend').replace('If true don\'t','Disable to not').replace('Defaults to: `false`','Enabled by default')"></span>
                                        <span class="value"> : {{ isEnabled(db.noKillBackend, true) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Analyze</strong>
                                         <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.noAnalyze').replace('If true don\'t','Disable to not').replace('Defaults to: `false`','Enabled by default')"></span>
                                        <span class="value"> : {{ isEnabled(db.noAnalyze, true) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Exclude Extension</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.excludeExtension')"></span>
                                        <span class="value"> : {{ isEnabled(db.excludeExtensions) }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Wait Timeout</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.spec.repack.waitTimeout')"></span>
                                        <span class="value"> : {{ db.waitTimeout ? getIsoDuration(db.waitTimeout) : 'No Timeout'}}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="crd.data.hasOwnProperty('status')">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li v-if="crd.data.status.hasOwnProperty('opStarted')">
                        <strong class="label">Operation Started</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.opStarted')"></span>
                        <span class="value timestamp"> : 
                            <span class='date'>
                                {{ crd.data.status.opStarted | formatTimestamp('date') }}
                            </span>
                            <span class='time'>
                                {{ crd.data.status.opStarted | formatTimestamp('time') }}
                            </span>
                            <span class='tzOffset'>{{ showTzOffset() }}</span>
                        </span>
                    </li>
                    <li v-if="crd.data.status.hasOwnProperty('opRetries')">
                        <strong class="label"> Operation Retries</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.opRetries')"></span>
                        <span colspan="value"> : {{ crd.data.status.opRetries }}</span>
                    </li>

                    <template v-if="((crd.data.spec.op === 'benchmark') && crd.data.status.hasOwnProperty('benchmark') && crd.data.status.benchmark.hasOwnProperty('pgbench'))">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">PgBench</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench')"></span>
                            <ul>
                                <li v-if="crd.data.status.benchmark.pgbench.hasOwnProperty('scaleFactor')">
                                    <strong class="label">Scale Factor</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.scaleFactor')"></span>
                                    <span class="value"> : {{ crd.data.status.benchmark.pgbench.scaleFactor }}</span>
                                </li>
                                <li v-if="crd.data.status.benchmark.pgbench.hasOwnProperty('transactionsProcessed')">
                                    <strong class="label">Transactions Processed</strong>
                                    <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsProcessed')"></span>
                                    <span class="value"> : {{ crd.data.status.benchmark.pgbench.transactionsProcessed }}</span>
                                </li>
                                <template v-if="( hasProp(crd, 'data.status.benchmark.pgbench.latency.average.value') || hasProp(crd, 'data.status.benchmark.pgbench.latency.standardDeviation.value') )">
                                    <li>
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Latency</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latency')"></span>
                                        <ul>
                                            <li v-if="hasProp(crd, 'data.status.benchmark.pgbench.latency.average.value')">
                                                <strong class="label">Average</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latency.average')"></span>
                                                <span class="value"> : {{ crd.data.status.benchmark.pgbench.latency.average.value }} {{ crd.data.status.benchmark.pgbench.latency.average.unit }}</span>
                                            </li>
                                            <li v-if="hasProp(crd, 'data.status.benchmark.pgbench.latency.standardDeviation.value')">
                                                <strong class="label">Standard Deviation</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.latency.standardDeviation')"></span>
                                                <span class="value"> : {{ crd.data.status.benchmark.pgbench.latency.standardDeviation.value }} {{ crd.data.status.benchmark.pgbench.latency.standardDeviation.unit }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                </template>
                                <template v-if="( hasProp(crd, 'data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.value') || hasProp(crd, 'data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.value') )">
                                    <li>
                                        <button class="toggleSummary"></button>
                                        <strong class="label">Transactions per Second</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsPerSecond')"></span>
                                        <ul>
                                            <li v-if="hasProp(crd, 'data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.value')">
                                                <strong class="label">Including Connections Establishing</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing')"></span>
                                                <span class="value"> : {{ crd.data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.value }} {{ crd.data.status.benchmark.pgbench.transactionsPerSecond.includingConnectionsEstablishing.unit }}</span>
                                            </li>
                                            <li v-if="hasProp(crd, 'data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.value')">
                                                <strong class="label">Excluding Connections Establishing</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing')"></span>
                                                <span class="textRight"> : {{ crd.data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.value }} {{ crd.data.status.benchmark.pgbench.transactionsPerSecond.excludingConnectionsEstablishing.unit }}</span>
                                            </li>
                                        </ul>
                                    </li>
                                </template>
                            </ul>
                        </li>

                    </template>

                    <template v-if="crd.data.status.hasOwnProperty(crd.data.spec.op)">
                         <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('primaryInstance')">
                            <strong class="label">Primary Instance</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.primaryInstance')"></span>
                            <span class="value"> : {{ crd.data.status[crd.data.spec.op].primaryInstance }}</span>
                        </li>
                        <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('initialInstances') && crd.data.status[crd.data.spec.op].initialInstances.length">
                            <button class="toggleSummary" v-if="crd.data.status[crd.data.spec.op].initialInstances.length > 1"></button>
                            <strong class="label">Initial Instances</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.initialInstances')"></span>
                            <template v-if="crd.data.status[crd.data.spec.op].initialInstances.length > 1">
                                <ul>
                                    <li v-for="instance in crd.data.status[crd.data.spec.op].initialInstances">
                                        {{ instance }}
                                    </li>
                                </ul>
                            </template>
                            <template v-else>
                                <span class="value"> : {{ crd.data.status[crd.data.spec.op].initialInstances[0] }}</span>
                            </template>
                        </li>

                        <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('pendingToRestartInstances') && crd.data.status[crd.data.spec.op].pendingToRestartInstances.length">
                            <button class="toggleSummary" v-if="crd.data.status[crd.data.spec.op].pendingToRestartInstances.length > 1"></button>
                            <strong class="label">Pending To Restart Instances</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.pendingToRestartInstances')"></span>
                            <template v-if="crd.data.status[crd.data.spec.op].pendingToRestartInstances.length > 1">
                                <ul>
                                    <li v-for="instance in crd.data.status[crd.data.spec.op].pendingToRestartInstances">
                                        {{ instance }}
                                    </li>
                                </ul>
                            </template>
                            <template v-else>
                                <span class="value"> : {{ crd.data.status[crd.data.spec.op].pendingToRestartInstances[0] }}</span>
                            </template>
                        </li>
                        <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('restartedInstances') && crd.data.status[crd.data.spec.op].restartedInstances.length">
                            <button class="toggleSummary" v-if="crd.data.status[crd.data.spec.op].restartedInstances.length > 1"></button>
                            <strong class="label">Restarted Instances</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.restartedInstances')"></span>
                            <template v-if="crd.data.status[crd.data.spec.op].restartedInstances.length > 1">
                                <ul>
                                    <li v-for="instance in crd.data.status[crd.data.spec.op].restartedInstances">
                                        {{ instance }}
                                    </li>
                                </ul>
                            </template>
                            <template v-else>
                                <span class="value"> : {{ crd.data.status[crd.data.spec.op].restartedInstances[0] }}</span>
                            </template>
                        </li>
                        <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('switchoverInitiated')">
                            <strong class="label">Switchover Initiated</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.switchoverInitiated')"></span>
                            <span class="value timestamp"> : 
                                <span class='date'>
                                    {{ crd.data.status[crd.data.spec.op].switchoverInitiated | formatTimestamp('date') }}
                                </span>
                                <span class='time'>
                                    {{ crd.data.status[crd.data.spec.op].switchoverInitiated | formatTimestamp('time') }}
                                </span>
                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                            </span>
                        </li>
                        <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('switchoverFinalized')">
                            <strong class="label">Switchover Finalized</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.switchoverFinalized')"></span>
                            <span class="value timestamp"> :
                                <span class='date'>
                                    {{ crd.data.status[crd.data.spec.op].switchoverFinalized | formatTimestamp('date') }}
                                </span>
                                <span class='time'>
                                    {{ crd.data.status[crd.data.spec.op].switchoverFinalized | formatTimestamp('time') }}
                                </span>
                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                            </span>
                        </li>
                        <li v-if="crd.data.status[crd.data.spec.op].hasOwnProperty('failure')">
                            <strong class="label">Failure</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.' + crd.data.spec.op + '.failure')"></span>
                            <span class="value"> : {{ crd.data.status[crd.data.spec.op].failure }}</span>
                        </li>
                    </template>

                    <template v-if="crd.data.status.hasOwnProperty('conditions')">
                        <li>
                            <button class="toggleSummary"></button>
                            <strong class="label">Conditions</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions')"></span> 
                            <ul>
                                <template v-for="condition in crd.data.status.conditions">
                                    <li>
                                        <button class="toggleSummary"></button>
                                        <strong class="label">{{ condition.type }}</strong>
                                        <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.type')"></span>
                                        <ul>
                                            <li v-if="condition.hasOwnProperty('lastTransitionTime')">
                                                <strong class="label">Last Transition Time</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.lastTransitionTime')"></span>
                                                <span class="value timestamp"> :
                                                    <span class='date'>
                                                        {{ condition.lastTransitionTime | formatTimestamp('date') }}
                                                    </span>
                                                    <span class='time'>
                                                        {{ condition.lastTransitionTime | formatTimestamp('time') }}
                                                    </span>
                                                    <span class='tzOffset'>{{ showTzOffset() }}</span>
                                                </span>
                                            </li>
                                            <li v-if="condition.hasOwnProperty('reason')">
                                                <strong class="label">Reason</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.reason')"></span>
                                                <span class="value"> : {{ condition.reason }}</span>
                                            </li>
                                            <li v-if="condition.hasOwnProperty('status')">
                                                <strong class="label">Status</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.status')"></span>
                                                <span> : {{ condition.status }}</span>
                                            </li>
                                            <li v-if="condition.hasOwnProperty('message')">
                                                <strong class="label">Message</strong>
                                                <span class="helpTooltip" :data-tooltip="getTooltip('sgdbops.status.conditions.message')"></span>
                                                <span> : {{ condition.message }}</span>
                                            </li>
                                        </ul>
                                       
                                    </li>
                                </template>
                            </ul>
                        </li>
                    </template>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import {mixin} from '../../mixins/mixin'
    import store from '../../../store'
    
    export default {
        name: 'SGDbOpsSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],

        methods: {
            hasTimedOut(crd) {
                if( crd.data.hasOwnProperty('status') ) {
                    let failedOp = crd.data.status.conditions.find(c => (c.status === 'True') && (c.type == 'Failed') && (c.reason == 'OperationTimedOut' ) )

                    if( typeof failedOp !== 'undefined' )
                        return 'YES'
                    else
                        return 'NO'

                } else {
                    return 'NO'
                }
            },
        },

        computed : {
            timezone () {
                return store.state.timezone
            },
        }
	}
</script>