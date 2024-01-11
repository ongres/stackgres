<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Metadata </strong>
                <ul>
                    <li v-if="showDefaults">
                        <strong class="label">Namespace</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.namespace')"></span>
                        <span class="value"> : {{ crd.data.metadata.namespace }}</span>
                    </li>
                    <li>
                        <strong class="label">Name</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.name')"></span>
                        <span class="value"> : {{ crd.data.metadata.name }}</span>
                    </li>
                    <li v-if="hasProp(crd, 'data.metadata.uid')">
                        <strong class="label">UID</strong>
                        <span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.metadata.uid')"></span>
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
                        <strong class="label">Source Cluster</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.spec.sgCluster')"></span>
                        <span class="value"> : 
                            <router-link :to="'/' + $route.params.namespace + '/sgcluster/' + crd.data.spec.sgCluster" target="_blank"> 
                                {{ crd.data.spec.sgCluster }}
                                <span class="eyeIcon"></span>
                            </router-link>
                        </span>
                    </li>
                    <li v-if="showDefaults || crd.data.spec.managedLifecycle">
                        <strong class="label">Managed Lifecycle (request)</strong>
                        <span  class="helpTooltip" :data-tooltip="getTooltip('sgbackup.spec.managedLifecycle')"></span>
                        <span class="value"> : {{ isEnabled(crd.data.spec.managedLifecycle) }}</span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul class="section" v-if="(hasProp(crd, 'data.status') && Object.keys(crd.data.status).length > 0)">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">Status </strong>
                <ul>
                    <li>
                        <strong class="label">Status</strong>
                        <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.status')"></span>
                        <span class="value"> : {{ crd.data.status.process.status }}</span>
                    </li>

                    <template v-if="crd.data.status.process.status === 'Completed'">
                        <li>
                            <strong class="label">Size uncompressed</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.size.uncompressed')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.size.uncompressed | formatBytes }}</span>
                        </li>
                        <li>
                            <strong class="label">Size compressed</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.size.compressed')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.size.compressed | formatBytes }}</span>
                        </li>
                        <li>
                            <strong class="label">PG</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.postgresVersion')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.postgresVersion | prefix }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.status.internalName')">
                            <strong class="label">Internal Name</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.internalName')"></span>
                            <span class="value"> : {{ crd.data.status.internalName }}</span>
                        </li>
                        <li>
                            <strong class="label">Start Time</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.start')"></span>
                            <span class="value timestamp"> : 
                                <span class='date'>
                                    {{ crd.data.status.process.timing.start | formatTimestamp('date') }}
                                </span>
                                <span class='time'>
                                    {{ crd.data.status.process.timing.start | formatTimestamp('time') }}
                                </span>
                                <span class='ms'>
                                    {{ crd.data.status.process.timing.start | formatTimestamp('ms') }}
                                </span>
                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                            </span>
                        </li>
                        <li>
                            <strong class="label">End Time</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.end')"></span>
                            <span class="value timestamp"> : 
                                <span class='date'>
                                    {{ crd.data.status.process.timing.end | formatTimestamp('date') }}
                                </span>
                                <span class='time'>
                                    {{ crd.data.status.process.timing.end | formatTimestamp('time') }}
                                </span>
                                <span class='ms'>
                                    {{ crd.data.status.process.timing.end | formatTimestamp('ms') }}
                                </span>
                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                            </span>
                        </li>
                        <li>
                            <strong class="label">Stored Time</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.timing.stored')"></span>
                            <span class="value timestamp"> : 
                                <span class='date'>
                                    {{ crd.data.status.process.timing.stored | formatTimestamp('date') }}
                                </span>
                                <span class='time'>
                                    {{ crd.data.status.process.timing.stored | formatTimestamp('time') }}
                                </span>
                                <span class='ms'>
                                    {{ crd.data.status.process.timing.stored | formatTimestamp('ms') }}
                                </span>
                                <span class='tzOffset'>{{ showTzOffset() }}</span>
                            </span>
                        </li>
                        <li v-if="(crd.data.status.process.status === 'Completed' )" :set="duration = getBackupDuration(crd)">
                        <strong class="label">Elapsed</strong>
                            <span class="helpTooltip" data-tooltip="Total time transcurred between the start time of backup and the time at which the backup is safely stored in the object storage."></span>
                            <template v-if="duration.length">
                                <span class="value timestamp"> :
                                    <span class='time'>
                                        {{ duration | formatTimestamp('time') }}
                                    </span>
                                    <span class='ms'>
                                        {{ duration | formatTimestamp('ms') }}
                                    </span>
                                </span>
                            </template>
                            <template v-else>
                                <span class="value">
                                    : The time spent taking the backup could not be calculated
                                </span>
                            </template>
                        </li>
                        <li>
                            <strong class="label">LSN (start ⇢ end)</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.lsn.start') + ' ⇢ ' + getTooltip('sgbackup.status.backupInformation.lsn.end')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.lsn.start }} ⇢ {{ crd.data.status.backupInformation.lsn.end }}</span>
                        </li>
                        <li>
                            <strong class="label">Source Pod</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.sourcePod')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.sourcePod }}</span>
                        </li>
                        <li>
                            <strong class="label">Timeline</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.timeline')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.timeline }}</span>
                        </li>
                        <li>
                            <strong class="label">System Identifier</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.systemIdentifier')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.systemIdentifier }}</span>
                        </li>
                        <li>
                            <strong class="label">Job Pod</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.jobPod')"></span>
                            <span class="value"> : {{ crd.data.status.process.jobPod }}</span>
                        </li>
                        <li>
                            <strong class="label">Managed Lifecycle (status)</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.managedLifecycle')"></span>
                            <span class="value"> : {{ crd.data.status.process.managedLifecycle ? 'Enabled' : 'Disabled' }}</span>
                        </li>
                        <li>
                            <strong class="label">Hostname</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.hostname')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.hostname }}</span>
                        </li>
                        <li>
                            <strong class="label">PG Data</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.pgData')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.pgData }}</span>
                        </li>
                        <li>
                            <strong class="label">Start Wal File</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.startWalFile')"></span>
                            <span class="value"> : {{ crd.data.status.backupInformation.startWalFile }}</span>
                        </li>
                        <li v-if="(typeof crd.data.status.backupInformation.controlData !== 'undefined')" class="controlData">
                            <strong class="label">Control Data</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupInformation.controlData')"></span>
                            <span class="value">
                                <a @click="setContentTooltip('#controlData')"> 
                                    View Control Data
                                    <span class="eyeIcon"></span>
                                </a>

                                <div id="controlData" class="hidden">
                                    <div class="summary">
                                        <ul>
                                            <li>
                                                <button class="toggleSummary"></button>
                                                <strong class="label">Control Data</strong>
                                                <ul>
                                                    <li v-for="(value, key) in crd.data.status.backupInformation.controlData">
                                                        <strong class="label">{{ key }}</strong>
                                                        <span class="value"> : {{ value }}</span>
                                                    </li>
                                                </ul>
                                            </li>
                                        </ul>
                                    </div>
                                </div>
                            </span>
                        </li>
                        <li v-if="hasProp(crd, 'data.status.backupPath')">
                            <strong class="label">Backup Path</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.backupPath')"></span>
                            <span class="value"> : {{ crd.data.status.backupPath }}</span>
                        </li>
                        <li v-if="hasProp(crd, 'data.status.volumeSnapshot')">
                            <button class="toggleSummary"></button>
                            <strong class="label">
                                Volume Snapshot
                            </strong>
                            <span
                                class="helpTooltip"
                                :data-tooltip="getTooltip('sgbackup.status.volumeSnapshot')"
                            ></span>

                            <ul>
                                <li v-if="hasProp(crd, 'data.status.volumeSnapshot.name')">
                                    <strong class="label">
                                        Name
                                    </strong>
                                    <span
                                        class="helpTooltip"
                                        :data-tooltip="getTooltip('sgbackup.status.volumeSnapshot.name')"
                                    ></span>
                                    <span class="value"> : {{ crd.data.status.volumeSnapshot.name }}</span>
                                </li>
                                <li v-if="hasProp(crd, 'data.status.volumeSnapshot.backupLabel')">
                                    <strong class="label">
                                        Backup Label
                                    </strong>
                                    <span
                                        class="helpTooltip"
                                        :data-tooltip="getTooltip('sgbackup.status.volumeSnapshot.backupLabel')"
                                    ></span>
                                    <span class="value">
                                        <a @click="setContentTooltip('#backupLabel')"> 
                                            View Backup Label
                                            <span class="eyeIcon"></span>
                                        </a>

                                        <div id="backupLabel" class="hidden">
                                            <pre>
                                                {{ crd.data.status.volumeSnapshot.backupLabel }}
                                            </pre>
                                        </div>
                                    </span>
                                </li>
                                <li v-if="hasProp(crd, 'data.status.volumeSnapshot.tablespaceMap')">
                                    <strong class="label">
                                        Tablespace Map
                                    </strong>
                                    <span
                                        class="helpTooltip"
                                        :data-tooltip="getTooltip('sgbackup.status.volumeSnapshot.tablespaceMap')"
                                    ></span>
                                    <span class="value"> : {{ crd.data.status.volumeSnapshot.tablespaceMap }}</span>
                                </li>
                            </ul>
                        </li>
                    </template>

                    <template v-else-if="crd.data.status.process.status === 'Failed'">
                        <li>
                            <strong class="label">Failure Cause</strong>
                            <span class="helpTooltip" :data-tooltip="getTooltip('sgbackup.status.process.failure')"></span>
                            <span class="value"> : {{crd.data.status.process.failure}}</span>
                        </li>
                    </template>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import {mixin} from '../../mixins/mixin'
    import moment from 'moment'

    export default {
        name: 'SGBackupSummary',

        mixins: [mixin],
        
        props: ['crd', 'showDefaults'],

        methods: {
            getBackupDuration( crd ) {
				if ( this.hasProp(crd, 'data.status.process.timing.start') && this.hasProp(crd, 'data.status.process.timing.stored') ) {
					let start = moment(crd.data.status.process.timing.start);
					let finish = moment(crd.data.status.process.timing.stored);

					return new Date(moment.duration(finish.diff(start))).toISOString();
				} else {
					return '';
				}
			},
        }
	}
</script>