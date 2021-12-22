<template>
    <div>
        <ul class="section">
            <li>
                <strong class="sectionTitle">Backup Configuration</strong>
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
                        <strong class="label">Cron Schedule:</strong>
                        <span class="value">{{ tzCrontab(crd.data.spec.baseBackups.cronSchedule) }} ({{ tzCrontab(crd.data.spec.baseBackups.cronSchedule) | prettyCRON(false) }})</span>
                    </li>
                </ul>
            </li>
        </ul>
    </div>
</template>

<script>
    import {mixin} from '../../mixins/mixin'

    export default {
        name: 'SGBackupConfigSummary',
        
        mixins: [mixin],

        props: ['crd', 'showDefaults']
	}
</script>