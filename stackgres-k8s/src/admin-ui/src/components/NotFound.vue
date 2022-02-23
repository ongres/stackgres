<template>
    <div>
        <div id="notFound" v-if="loggedIn">
            <h1>Not Found</h1>
            <p>
                The resource you're looking for doesn't exist,<br/>
                confirm your URL is correct and try again
            </p>
            <br/>
            <router-link to="/" class="btn">Go to Homepage</router-link>
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