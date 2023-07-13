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
                if (store.state.hasOwnProperty('loginToken'))
                    return store.state.loginToken.length > 0
                else
                    return false
            } 

        }
    }
</script>

<style scoped>
	body:not(.collapsed) #nav #notFound {
		width: calc(100vw - 350px);
		margin-left: 350px;
	}

    #notFound h1 {
        font-size: 2rem;
        margin-bottom: 10px;
    }

    #notFound p {
        margin-bottom: 20px;
        font-size: 1rem;
    }
</style>