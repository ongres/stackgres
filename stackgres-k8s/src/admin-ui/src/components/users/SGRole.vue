<template>
    <div id="SGRole" v-if="iCanLoad">
        <div class="content">
            <h2>
                {{ splitUppercase(roleKind) }} Details
                <router-link
                    v-if="iCan('patch', roleKind.toLowerCase(), (roleKind === 'Roles') ? crd.data.metadata.namespace : 'any')"
                    :to="'/manage/' + $route.meta.componentName.toLowerCase() + '/' + crd.data.metadata.name + '/edit'"
                    :title="'Edit ' + splitUppercase(roleKind)"
                    class="floatRight"
                >
                    EDIT
                </router-link>
            </h2>
            <template v-if="(typeof crd.data !== 'undefined')">
                <CRDSummary :crd="crd" kind="SGRole" :details="true"></CRDSummary>
            </template>
        </div>
    </div>
</template>

<script>
    import sgApi from '../../api/sgApi';
    import store from '../../store';
    import CRDSummary from '../forms/summary/CRDSummary.vue';
    import { mixin } from '../mixins/mixin';
    
    export default {
        name: 'SGRole',

        mixins: [mixin],

        components: {
            CRDSummary
        },

        computed: {

            // Define kind (role or clusterrole) according to the route's component name
            roleKind() {
                return this.$route.meta.componentName + 's';
            },
            
            crd() {
                return {
                    data: store.state[this.roleKind.toLowerCase()].find( role => (
                        role.metadata.name === this.$route.params.name
                    ))
                        
                }
            }

        },

        mounted() {
            if(typeof this.crd.data === 'undefined') {
                this.initData();
            }
        },

        methods: {

            initData() {
                const vc = this;
                
                sgApi
                .get(vc.roleKind.toLowerCase())
                .then( (response) => {
                    store.commit('set' + vc.roleKind, response.data)
                })
                .catch(function(err) {
                    console.log(err);
                    vc.checkAuthError(err);
                });
            }

        }
        
    }
</script>