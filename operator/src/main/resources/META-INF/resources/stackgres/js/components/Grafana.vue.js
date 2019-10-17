var Grafana = Vue.component("grafana", {
	template: `
		<div id="grafana">
			<header>
				<h2 class="title">Grafana Report</h2>
				<h3 class="subtitle">{{ $route.params.pod }}</h3>

				<ul class="selector">
					<li><strong>Select a node:</strong></li>
					<li v-for="pod in pods">
						<router-link :to="'/grafana/'+$route.params.name+'/'+pod.ip+':9187'" class="item">{{ pod.name }}</router-link>
					</li>
				</ul>
			</header>

			<div class="content grafana">
				<iframe :src="grafana"></iframe>				
			</div>
		</div>`,
	data: function() {

		return {
			pods: [],
			grafana: ""
		}
	},
	created: function() {
		this.fetchData()
	},
	computed: function() {
		this.fetchData()
	},
  	watch: {
    	'$route': 'fetchData'
  	},
  	methods: {
	    fetchData () {
	      // Grafana service
			let vm = this;
			
			$.get("http://localhost:8080/grafana", function(data) {
			  vm.grafana = data;
			  vm.grafana += '&theme=light&kiosk';
			  //vm.grafana += '&var-instance='+vm.$route.params.pod;
			  //alert("POD0: "+clustersData[this.$route.params.name].data.status.pods[0].ip);
			  //vm.grafana += '&var-instance='+this.$route.params.pod;
			  //vm.grafana += '&var-instance=10.244.2.11:9187';
			});

			vm.pods = clustersData[this.$route.params.name].data.status.pods;

			if(this.$route.params.pod.length)
				$(".grafana iframe").prop("src",vm.grafana+'&var-instance='+this.$route.params.pod);
			else
				$(".grafana iframe").prop("src",vm.grafana+'&var-instance='+vm.pods.first().ip+':9187');
			
	    }
	}
})
