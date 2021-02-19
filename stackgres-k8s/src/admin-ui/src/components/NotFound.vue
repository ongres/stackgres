<template>
    <div>
        <div id="notFound" v-if="loggedIn">
            <h1>Not Found</h1>
            <p>The resource you're looking for doesn't exist</p>
            <router-link to="/overview/default" class="btn">Go to Default Dashboard</router-link>
        </div>
    </div>
</template>

<script>
    import store from '../store'

    export default {
        name: 'NotFound',

        computed: {

            loggedIn () {
                return store.state.loginToken.length
            } 

        }
    }
</script>

<style scoped>
    #notFound h1 {
        font-size: 2rem;
        margin-bottom: 10px;
    }

    #notFound p {
        margin-bottom: 20px;
        font-size: 1rem;
    }
</style>
