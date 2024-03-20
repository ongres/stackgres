<template>
    <div>
        <ul class="section">
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">
                    Metadata
                </strong>
                <ul>
                    <li>
                        <strong class="label">
                            Namespace
                        </strong>
                        <span
                            class="helpTooltip"
                            :data-tooltip="getTooltip('sgrole.metadata.namespace')"
                        ></span>
                        <span class="value">
                             : {{ crd.data.metadata.namespace }}
                        </span>
                    </li>
                    <li>
                        <strong class="label">
                            Name
                        </strong>
                        <span
                            class="helpTooltip"
                            :data-tooltip="getTooltip('sgrole.metadata.name')"
                        ></span>
                        <span class="value">
                             : {{ crd.data.metadata.name }}
                        </span>
                    </li>
                </ul>
            </li>
        </ul>

        <ul
            class="section"
            v-if="crd.data.hasOwnProperty('rules') && !isNull(crd.data.rules)"
        >
            <!--CRD Details-->
            <li>
                <button class="toggleSummary"></button>
                <strong class="sectionTitle">
                    Rules
                    <span
                        class="helpTooltip"
                        :data-tooltip="getTooltip('sgrole.rules')"
                    ></span>
                </strong>
                <ul>
                    <template v-for="(rule, ruleIndex) in crd.data.rules">
                        <li :key="'rule[' + ruleIndex + ']'">
                            <button class="toggleSummary"></button>
                            <strong class="label">
                                Rule #{{ ruleIndex + 1}}
                            </strong>
                            <ul>
                                <template v-for="(ruleSet, ruleSetName) in rule">
                                    <template v-if="!isNull(ruleSet)">
                                        <li :key="'rule[' + ruleIndex + '].' + ruleSetName">
                                            <button class="toggleSummary"></button>
                                            <strong class="label">
                                                {{ splitUppercase(ruleSetName).replace(/([A-Z]) /g, '$1').trim() }}
                                            </strong>
                                            <span
                                                class="helpTooltip"
                                                :data-tooltip="getTooltip('sgrole.rules.' + ruleSetName)"
                                            ></span>

                                            <ul>
                                                <template v-for="(ruleSetItem, ruleSetItemIndex) in ruleSet">
                                                    <li :key="'rule[' + ruleIndex + '].' + ruleSetName + '[' + ruleSetItemIndex + ']'">
                                                        <span class="value">
                                                            {{ !ruleSetItem.length ? '""' : ruleSetItem }}
                                                        </span>
                                                    </li>
                                                </template>
                                            </ul>
                                        </li>
                                    </template>
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
    import { mixin } from './../../mixins/mixin'

    export default {
        name: 'SGRoleSummary',

        mixins: [mixin],

        props: ['crd', 'showDefaults'],
	}
</script>