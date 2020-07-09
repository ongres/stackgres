var NotFound = Vue.component("sg-not-found", {
    template: `<div></div>`,
	data: function() {
        return {}
	},
	beforeCreate: function() {
        if(window.location.pathname !== '/admin/index.html')
            window.location.href = '/admin/not-found.html';
    }
})

