var NotFound = Vue.component("sg-not-found", {
    template: `<div>
            <div id="notFound" v-if="loggedIn">
                <h1>Not Found</h1>
                <p>
                    The resource you're looking for doesn't exist,<br/>
                    confirm your URL is correct and try again
                </p>
                <br/>
                <router-link to="/admin/overview/default" class="btn">Go to Default Dashboard</router-link>
            </div>
        </div>`,
	data: function() {
        return {}
	},
	computed: {

        loggedIn () {
            return store.state.loginToken.length
        } 

    }
})