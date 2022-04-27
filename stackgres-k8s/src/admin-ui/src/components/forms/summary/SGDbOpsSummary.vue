<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Operation</strong>
                <ul>
                    <li>
                        <strong class="sectionTitle">Metadata</strong>
                        <ul>
                            <li v-if="showDefaults">
                                <strong class="label">Namespace:</strong>
                                <span class="value">{{ crd.data.metadata.namespace }}</span>
                            </li>
                            <li>
                                <strong class="label">Name:</strong>
                                <span class="value">{{ crd.data.metadata.name }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section">
            <li>
                <strong class="sectionTitle">Specs</strong>
                <ul>
                    <li>
                        <strong class="label">Operation:</strong>
                        <span class="value capitalize">{{ splitUppercase(crd.data.spec.op) }}</span>
                    </li>
                    <li>
                        <strong class="label">Source Cluster:</strong>
                        <span class="value">
                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + crd.data.spec.sgCluster" target="_blank"> 
                                {{ crd.data.spec.sgCluster }}
                            </router-link>
                        </span>
                    </li>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.runAt') )">
                        <strong class="label">Run At:</strong>
                        
                        <span class="value timestamp" v-if="hasProp(crd, 'data.spec.runAt')">
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
                        <span class="value" v-else>
                            ASAP
                        </span>
                    </li>
                    <li v-if="( showDefaults || hasProp(crd, 'data.spec.timeout') )">
                        <strong class="label">Timeout:</strong>
                        <span class="value">{{ hasProp(crd, 'data.spec.timeout') ? getIsoDuration(crd.data.spec.timeout) : 'NONE' }}</span>
                    </li>
                    <li v-if="( showDefaults || (crd.data.spec.maxRetries > 0) )">
                        <strong class="label">Maximum Retries:</strong>
                        <span class="value">{{ crd.data.spec.maxRetries }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( (crd.data.spec.op == 'restart') && ( showDefaults || ( (crd.data.spec.restart.method != 'InPlace') || crd.data.spec.restart.onlyPendingRestart ) ) )">
            <li>
                <strong class="sectionTitle">Restart Details</strong>
                <ul>
                    <li v-if="( showDefaults || (crd.data.spec.restart.method != 'InPlace') )">
                        <strong class="label">Method:</strong>
                        <span class="value">{{ splitUppercase(crd.data.spec.restart.method) }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.restart.onlyPendingRestart )">
                        <strong class="label">Pods to Restart:</strong>
                        <span class="value">{{ crd.data.spec.restart.onlyPendingRestart ? 'Pending pods only' : 'All pods' }}</span>
                    </li>
                </ul>
            </li>
        </ul>
        
        <ul class="section" v-else-if="crd.data.spec.op == 'securityUpgrade'">
            <li>
                <strong class="sectionTitle">Security Upgrade Details</strong>
                <ul>
                    <li>
                        <strong class="label">Method:</strong>
                        <span class="value">{{ splitUppercase(crd.data.spec.securityUpgrade.method) }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.op == 'minorVersionUpgrade')">
            <li>
                <strong class="sectionTitle">Minor Version Upgrade Details</strong>
                <ul>
                    <li v-if="( showDefaults || (crd.data.spec.minorVersionUpgrade.method != 'InPlace') )">
                        <strong class="label">Method:</strong>
                        <span class="value">{{ splitUppercase(crd.data.spec.minorVersionUpgrade.method) }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Postgres Version:</strong>
                        <span class="value">{{ crd.data.spec.minorVersionUpgrade.postgresVersion }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(crd.data.spec.op == 'majorVersionUpgrade')">
            <li>
                <strong class="sectionTitle">Major Version Upgrade Details</strong>
                <ul>
                    <li v-if="( showDefaults || crd.data.spec.majorVersionUpgrade.link )">
                        <strong class="label">Hard Link Files:</strong>
                        <span class="value">{{ crd.data.spec.majorVersionUpgrade.link ? 'YES' : 'NO' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.majorVersionUpgrade.clone )">
                        <strong class="label">Clone Files:</strong>
                        <span class="value">{{ crd.data.spec.majorVersionUpgrade.clone ? 'YES' : 'NO' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.majorVersionUpgrade.check )">
                        <strong class="label">Check Clusters:</strong>
                        <span class="value">{{ crd.data.spec.majorVersionUpgrade.check ? 'YES' : 'NO' }}</span>
                    </li>
                    <li v-if="crd.data.spec.majorVersionUpgrade.backupPath.length">
                        <strong class="label">Backup Path:</strong>
                        <span class="value">{{ crd.data.spec.majorVersionUpgrade.backupPath }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Postgres Version:</strong>
                        <span class="value">{{ crd.data.spec.majorVersionUpgrade.postgresVersion }}</span>
                    </li>
                    <li>
                        <strong class="label">Target Postgres Configuration:</strong>
                        <span class="value">
                            <router-link :to="'/' + $route.params.namespace + '/sgpgconfig/' + crd.data.spec.majorVersionUpgrade.sgPostgresConfig" target="_blank"> 
                                {{ crd.data.spec.majorVersionUpgrade.sgPostgresConfig }}
                            </router-link>
                        </span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( (crd.data.spec.op == 'vacuum') && ( showDefaults || (crd.data.spec.vacuum.full || crd.data.spec.vacuum.freeze || crd.data.spec.vacuum.analyze || crd.data.spec.vacuum.disablePageSkipping) ) )">
            <li>
                <strong class="sectionTitle">Vacuum Details</strong>
                <ul>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.full )">
                        <strong class="label">Full Vacuum:</strong>
                        <span class="value">{{ crd.data.spec.vacuum.full ? 'YES' : 'NO' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.freeze )">
                        <strong class="label">Freeze:</strong>
                        <span class="value">{{ crd.data.spec.vacuum.freeze ? 'YES' : 'NO' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.analyze )">
                        <strong class="label">Analyze:</strong>
                        <span class="value">{{ crd.data.spec.vacuum.analyze ? 'YES' : 'NO' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.vacuum.disablePageSkipping )">
                        <strong class="label">Page Skipping:</strong>
                        <span class="value">{{ crd.data.spec.vacuum.disablePageSkipping ? 'NO' : 'YES' }}</span>
                    </li>
                    
                    <li v-if="hasProp(crd, 'data.spec.vacuum.databases')">
                        <strong class="sectionTitle">Database Specific Specs</strong>
                        <ul>
                            <li v-for="db in crd.data.spec.vacuum.databases">
                                <strong class="sectionTitle">
                                    Database: 
                                    <span class="value">{{ db.name }}</span>
                                </strong>
                                <ul>
                                    <li>
                                        <strong class="label">Full Vacuum:</strong>
                                        <span class="value">{{ db.full ? 'YES' : 'NO' }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Freeze:</strong>
                                        <span class="value">{{ db.freeze ? 'YES' : 'NO' }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Analyze:</strong>
                                        <span class="value">{{ db.analyze ? 'YES' : 'NO' }}</span>
                                    </li>
                                    <li>
                                        <strong class="label">Page Skipping:</strong>
                                        <span class="value">{{ db.disablePageSkipping ? 'NO' : 'YES' }}</span>
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
                <strong class="sectionTitle">Benchmark Details</strong>
                <ul>
                    <li>
                        <strong class="label">Type:</strong>
                        <span class="value">{{ crd.data.spec.benchmark.type }}</span>
                    </li>
                    <li v-if="( showDefaults || (crd.data.spec.benchmark.connectionType != 'primary-service') )">
                        <strong class="label">Connection Type:</strong>
                        <span class="value">{{ (crd.data.spec.benchmark.connectionType == 'primary-service') ? 'Primary Service' : 'Replicas Service' }}</span>
                    </li>

                    <li v-if="(crd.data.spec.benchmark.type == 'pgbench')">
                        <strong class="sectionTitle">PgBench Details</strong>
                        <ul>
                            <li v-if="( showDefaults || (crd.data.spec.benchmark.pgbench.databaseSize != '1Gi') )">
                                <strong class="label">Database Size:</strong>
                                <span class="value">{{ crd.data.spec.benchmark.pgbench.databaseSize }}</span>
                            </li>
                            <li v-if="( showDefaults || crd.data.spec.benchmark.pgbench.usePreparedStatements )">
                                <strong class="label">Prepared Statements:</strong>
                                <span class="value">{{ crd.data.spec.benchmark.pgbench.usePreparedStatements ? 'YES' : 'NO' }}</span>
                            </li>
                            <li v-if="( showDefaults || (crd.data.spec.benchmark.pgbench.concurrentClients > 1) )">
                                <strong class="label">Concurrent Clients:</strong>
                                <span class="value">{{ crd.data.spec.benchmark.pgbench.concurrentClients }}</span>
                            </li>
                            <li v-if="( showDefaults || (crd.data.spec.benchmark.pgbench.threads > 1) )">
                                <strong class="label">Threads:</strong>
                                <span class="value">{{ crd.data.spec.benchmark.pgbench.threads }}</span>
                            </li>
                            <li>
                                <strong class="label">Duration:</strong>
                                <span class="value">{{ getIsoDuration(crd.data.spec.benchmark.pgbench.duration) }}</span>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="( (crd.data.spec.op == 'repack') && ( showDefaults || (crd.data.spec.repack.noOrder || crd.data.spec.repack.waitTimeout || crd.data.spec.repack.noKillBackend || crd.data.spec.repack.noAnalyze || crd.data.spec.repack.excludeExtension || hasProp(crd, 'data.spec.repack.databases') ) ) )">
            <li>
                <strong class="sectionTitle">Repack Details</strong>
                <ul>
                    <li v-if="( showDefaults || crd.data.spec.repack.noOrder )">
                        <strong class="label">No Order:</strong>
                        <span class="value">{{ crd.data.spec.repack.noOrder ? 'ON' : 'OFF' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.repack.noKillBackend )">
                        <strong class="label">No Kill Backend:</strong>
                        <span class="value">{{ crd.data.spec.repack.noKillBackend ? 'ON' : 'OFF' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.repack.noAnalyze )">
                        <strong class="label">No Analyze:</strong>
                        <span class="value">{{ crd.data.spec.repack.noAnalyze ? 'ON' : 'OFF' }}</span>
                    </li>
                    <li v-if="( showDefaults || crd.data.spec.repack.excludeExtension )">
                        <strong class="label">Exclude Extension:</strong>
                        <span class="value">{{ crd.data.spec.repack.excludeExtension ? 'ON' : 'OFF' }}</span>
                    </li>
                    <li v-if="(showDefaults || crd.data.spec.repack.waitTimeout)">
                        <strong class="label">Wait Timeout:</strong>
                        <span class="value">{{ crd.data.spec.repack.waitTimeout ? getIsoDuration(crd.data.spec.repack.waitTimeout) : 'No Timeout'}}</span>
                    </li>
                    
                    <li v-if="hasProp(crd, 'data.spec.repack.databases')">
                        <strong class="sectionTitle">Database Specific Specs</strong>
                        <ul>
                            <li v-for="db in crd.data.spec.repack.databases">
                                <strong class="sectionTitle">
                                    Database: 
                                    <span class="value">{{ db.name }}</span>
                                </strong>
                                <ul v-if="( showDefaults || ( db.noOrder || db.waitTimeout || db.noKillBackend || db.noAnalyze || db.excludeExtension ) )">
                                    <li v-if="(showDefaults || db.noOrder)">
                                        <strong class="label">No Order:</strong>
                                        <span class="value">{{ db.noOrder ? 'ON' : 'OFF' }}</span>
                                    </li>
                                    <li v-if="(showDefaults || db.noKillBackend)">
                                        <strong class="label">No Kill Backend:</strong>
                                        <span class="value">{{ db.noKillBackend ? 'ON' : 'OFF' }}</span>
                                    </li>
                                    <li v-if="(showDefaults || db.noAnalyze)">
                                        <strong class="label">No Analyze:</strong>
                                        <span class="value">{{ db.noAnalyze ? 'ON' : 'OFF' }}</span>
                                    </li>
                                    <li v-if="(showDefaults || db.excludeExtension)">
                                        <strong class="label">Exclude Extension:</strong>
                                        <span class="value">{{ db.excludeExtensions ? 'ON' : 'OFF' }}</span>
                                    </li>
                                    <li v-if="(showDefaults || db.waitTimeout)">
                                        <strong class="label">Wait Timeout:</strong>
                                        <span class="value">{{ db.waitTimeout ? getIsoDuration(db.waitTimeout) : 'No Timeout'}}</span>
                                    </li>
                                </ul>
                            </li>
                        </ul>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import {mixin} from '../../mixins/mixin'
    
    export default {
        name: 'SGDbOpsSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults']
	}
</script>