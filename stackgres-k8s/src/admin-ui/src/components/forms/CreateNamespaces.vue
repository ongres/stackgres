<template>
    <div id="create-namespace" v-if="iCanLoad">
        <form id="createNamespace" class="form" @submit.prevent>
            <div class="header">
                <h2>
                    <span>Create Namespace</span>
                </h2>
            </div>

            <div class="row-50">
                <div class="col">
                    <label for="name">Namespace Name <span class="req">*</span></label>
                    <input v-model="name" required data-field="name" autocomplete="off">
                    <span class="helpTooltip" data-tooltip="Namespace name. Following Kubernetes naming conventions, it must be an alphanumeric (a-z, and 0-9) string, with a maximum length of 63 characters, with the - character allowed anywhere except the first or last character."></span>
                </div>
            </div>

             <span class="warning topLeft" v-if="nameColission && !editMode">
                There's already a <strong>Namespace</strong> with the same name you specified, please choose a different name.
            </span>

            <hr/>

            <button type="submit" class="btn" @click="createNamespace()">Create Namespace</button>
            <button @click="cancel()" class="btn border">Cancel</button>
        </form>
    </div>
</template>

<script>
    import {mixin} from '../mixins/mixin'
    import router from '../../router'
    import store from '../../store'
    import sgApi from '../../api/sgApi'

    export default {
        name: 'CreateNamespace',

        mixins: [mixin],
        
        data() {
            return {
                name: ''
            }
        },
        
        computed: {

            nameColission() {
                return store.state.namespaces.includes(this.name)
            }

        },
        methods: {

            createNamespace(preview = false, previous) {
                const vc = this;

                if(!vc.checkRequired()) {
                    return;
                }
               
                sgApi
                .createCustomResource('/namespaces/' + vc.name)
                .then(function (response) {
                    vc.notify('Namespace <strong>"' + vc.name + '"</strong> created successfully', 'message','namespaces');
                    vc.fetchAPI('namespaces');
                    router.push('/');
                })
                .catch(function (error) {
                    console.log(error.response);
                    vc.notify(error.response.data,'error','namespaces');
                });
            },

        }

    }
</script>