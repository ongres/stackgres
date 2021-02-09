<template>
    <div></div>
</template>

<script>

    export default {
        name: 'NotFound',

        data: function() {
            return {}
        },
        beforeCreate: function() {
            if(window.location.pathname !== '/admin/index.html')
                window.location.href = '/admin/not-found.html';
        }
    }
</script>
