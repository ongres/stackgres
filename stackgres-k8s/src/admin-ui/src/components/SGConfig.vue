<template>
    <div id="sgConfig" v-if="iCanLoad">
        <div class="content">
            <h2>Configuration Details</h2>
					
            <div class="configurationDetails">                      
                <CRDSummary :crd="crd" kind="SGConfig" :details="true"></CRDSummary>
            </div>
        </div>
    </div> 
</template>

<script>
    import store from '@/store'
    import { mixin } from './mixins/mixin'
    import CRDSummary from './forms/summary/CRDSummary.vue'
    
    export default {
        name: 'SGConfig',

		mixins: [mixin],

        components: {
            CRDSummary
        },

        data() {
            return {      
            }
        },

        computed: {
            crd() {
                return {
                    data: store.state.sgconfigs.find(c => c.metadata.name == this.$route.params.name)
                };
            }
        }
    }
</script>