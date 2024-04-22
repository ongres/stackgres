<template>
	<div
        id="crdSummary"
        class="visible loadingContainer"
        :class="[
            details ? 'crdDetails' : 'contentTooltip',
            !crd.hasOwnProperty('data') && 'loading'
        ]"
    >
        <div v-if="!details" class="close" @click="closeSummary()"></div>
        
        <div class="info">
        
            <span v-if="!details" class="close" @click="closeSummary()">CLOSE</span>
            
            <div class="content">
                <div v-if="!details" class="header">
                    <template v-if="dryRun">
                        <h2>Dry Run Results</h2>
                    </template>
                    <template v-else>
                        <h2>Summary</h2>
                    </template>
                    <label
                        v-if="!dryRun"
                        for="showDefaults" 
                        class="switch floatRight upper"
                    >
                        <span>Show Default Values</span>
                        <input type="checkbox" id="showDefaults" class="switch" v-model="showDefaults">
                    </label>
                </div>
                <div class="summary" v-if="crd.hasOwnProperty('data')">
                    <component :is="summaryComponent" :crd="crd" :showDefaults="showDefaults"></component>
                </div>
            </div>
        </div>
    </div>
</template>

<script>

    export default {
        name: 'CRDSummary',

        props: {
            kind: String,
            crd: Object,
            details: {
                type: Boolean,
                default: false
            },
            dryRun: {
                type: Boolean,
                default: false
            }
        },

        data() {
            return {
                showDefaults: this.dryRun || this.details
            }
        },

        computed: {

            summaryComponent() {
                const component = () => import('./' + this.kind + 'Summary.vue');
                return component
            }

        },

        methods: {
            closeSummary() {
                this.$emit('closeSummary', true)
            }
        }

	}
</script>

<style>

    .header {
        position: relative;
    }

    #crdSummary .header h2 {
        width: 100%;
    }

    .header label[for="showDefaults"] {
        border: 0;
        position: absolute;
        background: transparent;
        right: 0;
        bottom: 0;
        height: auto;
        display: inline-block;
        margin: 0;
        padding: 0;
    }

    .header label span {
        display: inline-block;
        margin-right: 5px;
    }

    .header .switch input {
        transform: translate(5px, 2px);
    }

    .header .switch input:before {
        display: none;
    }

	.sectionTitle {
        font-size: 1rem;
    }

    .summary {
        max-height: 50vh;
        overflow-y: auto;
        min-width: 500px;
    }

    .summary ul.section {
        margin: 10px 0 35px;
    }

    .summary ul:not(.section) {
        position: relative;
        padding: 15px 25px 0;
        list-style: none;
    }

    .summary strong.label + span[data-tooltip] {
        display: inline-block;
        margin-left: 7px;
    }

    .summary .value {
        font-weight: normal;
    }

    .summary li {
        margin-bottom: 10px;
        position: relative;
        list-style: none;
    }

    .summary ul li:last-child {
        margin-bottom: 5px;
    }

    .summary ul ul:before {
        content: "";
        display: inline;
        height: calc(100% - 12px);
        width: 2px;
        background: var(--borderColor);
        position: absolute;
        top: 5px;
        left: 5px;
    }

    .summary ul + ul {
        margin-top: -15px;
    }

    .summary ul ul ul li:last-of-type:after {
        content: "";
        display: inline;
        width: 4px;
        height: calc(100% + 7px);
        position: absolute;
        background: var(--bgColor);
        left: -20px;
        top: 8px;
    }

    .summary ul ul li:not(.warning):before {
        content: "";
        display: inline;
        height: 2px;
        width: 10px;
        position: absolute;
        background-color: var(--borderColor);
        left: -18px;
        top: 6px;
    }

    .summary ul.section > li > ul > li:last-child:after {
        content: " ";
        width: 2px;
        display: block;
        position: absolute;
        height: 100%;
        background: var(--bgColor);
        top: 8px;
        left: -20px;
        z-index: 3;
    }

    .crdDetails .summary {
        height: 100%;
        max-height: 100%;
    }

    .darkmode .summary ul ul ul li:last-of-type:after, .darkmode .summary ul.section > li > ul > li:last-child:after {
        background: var(--activeBg);
    }

    .darkmode .crdDetails .summary ul ul ul li:last-of-type:after, .darkmode .crdDetails .summary ul.section > li > ul > li:last-child:after {
        background: var(--bgColor);
    }

    span.arrow {
        background: "▾";
    }

    .summary button.toggleSummary {
        background: transparent;
        font-weight: bold;
        border: 0;
        margin: 0;
        padding: 10px;
        position: relative;
        top: -5px;
        left: -5px;
        margin-bottom: -6px;
        background: var(--activeBg);
        z-index: 1;
    }

    .crdDetails .summary button.toggleSummary {
        background: var(--bgColor);
    }

    .summary button.toggleSummary:before {
        content: "▾";
        display: block;
        position: absolute;
        top: 1px;
        left: 4px;
        width: 14px;
        color: var(--textColor);
        transition: all .3s ease-out;
    }

    .summary .collapsed button.toggleSummary:before {
        transform: rotate(-90deg);
        transition: all .3s ease-out;
    }

</style>