<template>
	<div id="crdSummary" class="contentTooltip show">
        <div class="close" @click="closeSummary()"></div>
        
        <div class="info">
        
            <span class="close" @click="closeSummary()">CLOSE</span>
            
            <div class="content">
                <div class="header">
                    <h2>Summary</h2>
                    <label for="showDefaults" class="switch floatRight upper">
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

        props: ['kind', 'crd'],

        data() {
            return {
                showDefaults: false,
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
        margin: 10px 0;
    }

    .summary ul {
        position: relative;
    }

    .summary ul ul {
        padding: 15px 25px 5px;
        list-style: none;
    }

    .summary strong.label {
        display: inline-block;
        margin-right: 7px;
    }

    .summary .value {
        font-weight: normal;
    }

    .summary li {
        margin-bottom: 10px;
        position: relative;
    }

    .summary ul li:last-child {
        margin-bottom: 5px;
    }

    .summary ul ul:before {
        content: "";
        display: inline;
        height: calc(100% - 5px);
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

    .summary ul ul li:before {
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

    .darkmode .summary ul ul ul li:last-of-type:after, .darkmode .summary ul.section > li > ul > li:last-child:after {
        background: var(--activeBg);
    }

</style>