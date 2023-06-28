<template>
	<div id="clusterDetails">
        <template v-if="cluster.hasOwnProperty('data')">           
            <div class="configurationDetails">
                <ClusterSummary :cluster="crd" :extensionsList="extensionsList" :details="true"></ClusterSummary>
            </div>
        </template>
    </div>
</template>

<script>
	import store from '../../store'
	import { mixin } from '../mixins/mixin'
    import ClusterSummary from './../forms/summary/SGClusterSummary.vue'

    export default {
        name: 'ClusterDetails',

		mixins: [mixin],

        components: {
            ClusterSummary
        },

        props: ['cluster', 'extensionsList'],

		methods: {

			unparseProps ( props, key = 'annotation' ) {
				var propsArray = [];
				if(!$.isEmptyObject(props)) {
					Object.entries(props).forEach(([k, v]) => {
						var prop = {};
						prop[key] = k;
						prop['value'] = v;
						propsArray.push(prop)
					});
				}
				
				return propsArray
			},

			countObjectArrayKeys(objectArray) {
				let count = 0;

				objectArray.forEach(function(obj, index) {
					count += Object.keys(obj).length
				})

				return count
			},

			sortExtensions(ext) {
				return [...ext].sort((a,b) => (a.name > b.name) ? 1 : ((b.name > a.name) ? -1 : 0))
			},

			clusterProfileMismatch(cluster, profile) {
				const vc = this;
				
				if(vc.hasProp(cluster, 'status.pods') && cluster.status.pods.length) {
					let mismatch = cluster.status.pods.find(pod => (( pod.cpuRequested != ( (pod.cpuRequested.includes('m') && !profile.data.spec.cpu.includes('m')) ? ( (profile.data.spec.cpu * 1000) + 'm' ) : profile.data.spec.cpu ) ) || (pod.memoryRequested.replace('.00','') != profile.data.spec.memory)))				
					return (typeof mismatch !== 'undefined')
				} else {
					return false;
				}

			},

			affinityOperator(op) {

				switch(op) {

					case 'NotIn':
						op = 'not in';
						break;
					case 'DoesNotExists':
						op = 'does not exists';
						break;
					case 'Gt':
						op = 'greather than';
						break;
					case 'Lt':
						op = 'less than';
						break;
				}

				return op.toLowerCase();

			},

            getReplicationRows(rep) {
                const vc = this;
                
                if(vc.hasProp(rep, 'instance.sgCluster')) {
                    return 2
                } else {
                    let count = 13;

                    if (vc.hasProp(rep, 'instance.external')) {
                        count += 2;
                    }

                    if(rep.hasOwnProperty('storage')) {
                        count += 2;

                        if(rep.storage.hasOwnProperty('performance')) {
                            count += Object.keys(rep.storage.performance).length
                        }
                    }
                    
                    return count
                }
            },

            getRowSpan(el) {
                let count = 0;

                if (Array.isArray(el)) {
                    el.forEach( (item) => {
                        count += this.getRowSpan(item);
                    })
                } else if (typeof el == 'object') {
                    Object.keys(el).forEach( (item) => {
                        count += this.getRowSpan(item) + 1;
                    })
                } 

                return count;
            }

		},

		computed: {

			tooltips () {
				return store.state.tooltips
			},

			profiles () {
				return store.state.sginstanceprofiles
			},

			backups () {
				return store.state.sgbackups
			},

            scripts () {
                return store.state.sgscripts
            },

            crd () {
				return store.state.sgclusters.find(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.metadata.name == this.$route.params.name))
			}
		},

        mounted: function() {

            const vc = this;

            $(document).on('click', '[data-content-tooltip]', (el) => {
                vc.setContentTooltip(el.target.dataset.contentTooltip);
            })

        }

	}
</script>

<style scoped>
	.clusterConfig td {
		position: relative;
	}

	.helpTooltip.alert {
		position: absolute;
		right: 30px;
		top: 12px;
        left: auto;
	}

	.postgresExtensions th {
		padding-left: 10px;
	}

	a.newTab {
    	background: none !important;
		color: var(--textColor);
	}

	a.newTab:after {
		position: relative;
		display: inline-block;
		content: "";
		height: 12px;
		width: 12px;
		left: 5px;
    	background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+PGcgZmlsbD0iIzE3MTcxNyI+PHBhdGggZD0iTS4zMDEgMTQuN2MtLjItLjItLjMtLjQtLjMtLjdWMy4yYzAtLjYuNS0xIDEtMWg1LjRjLjYgMCAxIC41IDEgMXMtLjUgMS0xIDFoLTQuNFYxM2g4LjhWOC42YzAtLjYuNS0xIDEtMXMxIC41IDEgMVYxNGMwIC42LS41IDEtMSAxaC0xMC44Yy0uMyAwLS41LS4xLS43LS4zeiIvPjxwYXRoIGQ9Ik0xNS4wMDEgNi40VjFjMC0uNi0uNS0xLTEtMWgtNS40Yy0uNiAwLTEgLjQtMSAxcy40IDEgMSAxaDIuOWwtNS4xIDUuMWMtLjQuNC0uNCAxIDAgMS40LjQuNCAxIC40IDEuNCAwbDUuMi01djIuOWMwIC42LjQgMSAxIDEgLjUuMSAxLS40IDEtMXoiLz48L2c+PC9zdmc+)  no-repeat !important;
	}

	.darkmode a.newTab:after {
		background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+PGcgZmlsbD0iI2ZmZmZmZiI+PHBhdGggZD0iTS4zMDEgMTQuN2MtLjItLjItLjMtLjQtLjMtLjdWMy4yYzAtLjYuNS0xIDEtMWg1LjRjLjYgMCAxIC41IDEgMXMtLjUgMS0xIDFoLTQuNFYxM2g4LjhWOC42YzAtLjYuNS0xIDEtMXMxIC41IDEgMVYxNGMwIC42LS41IDEtMSAxaC0xMC44Yy0uMyAwLS41LS4xLS43LS4zeiIvPjxwYXRoIGQ9Ik0xNS4wMDEgNi40VjFjMC0uNi0uNS0xLTEtMWgtNS40Yy0uNiAwLTEgLjQtMSAxcy40IDEgMSAxaDIuOWwtNS4xIDUuMWMtLjQuNC0uNCAxIDAgMS40LjQuNCAxIC40IDEuNCAwbDUuMi01djIuOWMwIC42LjQgMSAxIDEgLjUuMSAxLS40IDEtMXoiLz48L2c+PC9zdmc+)  no-repeat !important;	
	}

	a.newTab:hover:after {
		background: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxNSAxNSI+PGcgZmlsbD0iIzM2QThGRiI+PHBhdGggZD0iTS4zMDEgMTQuN2MtLjItLjItLjMtLjQtLjMtLjdWMy4yYzAtLjYuNS0xIDEtMWg1LjRjLjYgMCAxIC41IDEgMXMtLjUgMS0xIDFoLTQuNFYxM2g4LjhWOC42YzAtLjYuNS0xIDEtMXMxIC41IDEgMVYxNGMwIC42LS41IDEtMSAxaC0xMC44Yy0uMyAwLS41LS4xLS43LS4zeiIvPjxwYXRoIGQ9Ik0xNS4wMDEgNi40VjFjMC0uNi0uNS0xLTEtMWgtNS40Yy0uNiAwLTEgLjQtMSAxcy40IDEgMSAxaDIuOWwtNS4xIDUuMWMtLjQuNC0uNCAxIDAgMS40LjQuNCAxIC40IDEuNCAwbDUuMi01djIuOWMwIC42LjQgMSAxIDEgLjUuMSAxLS40IDEtMXoiLz48L2c+PC9zdmc+)  no-repeat !important;
	}

	.warning {
		background: rgba(0,173,181,.05);
		border: 1px solid var(--blue);
		padding: 20px;
		border-radius: 6px;
		display: inline-block;
		line-height: 1.1;
		width: 100%;
	}

	.trimText {
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		display: block;
		max-width: 250px;
		width: 100%;
	}

	th.textRight > span {
    	margin-right: 10px;
	}

    .eye-icon {
        padding-right: 29px;
    }

    .eye-icon:after {
        content: '';
        width: 18px;
        height: 14px;
        transform: scale(0.85);
        display: inline-block;
        position: absolute;
        right: 10px;
    }
</style>
