<template>
    <div>
        <div id="notFound">
            <h1>Not Found</h1>
            <p>The resource you're looking for doesn't exist</p>
            <router-link to="/overview/default" class="btn">Go to Default Dashboard</router-link>
        </div>
    </div>
</template>

<script>

    export default {
        name: 'NotFound'
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
