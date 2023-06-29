<template>
	<div id="cluster-config" v-if="iCanLoad">
		<div class="content noScroll">
			<h2>
				Cluster
				<template v-if="hasProp(sgshardedcluster, 'data.status.conditions')">
					<template v-for="condition in sgshardedcluster.data.status.conditions" v-if="( (condition.type == 'PendingRestart') && (condition.status == 'True') )">
						<span class="helpTooltip alert" data-tooltip="A restart operation is pending for this cluster"></span>
					</template>
				</template>
			</h2>
			
			<div class="connectionInfo" v-if="hasProp(sgshardedcluster, 'data.info')">
				<a @click="setContentTooltip('#connectionInfo', !podsReady)"> 
					<h2>View Connection Info</h2>
					<svg xmlns="http://www.w3.org/2000/svg" width="18.556" height="14.004" viewBox="0 0 18.556 14.004"><g transform="translate(0 -126.766)"><path d="M18.459,133.353c-.134-.269-3.359-6.587-9.18-6.587S.232,133.084.1,133.353a.93.93,0,0,0,0,.831c.135.269,3.36,6.586,9.18,6.586s9.046-6.317,9.18-6.586A.93.93,0,0,0,18.459,133.353Zm-9.18,5.558c-3.9,0-6.516-3.851-7.284-5.142.767-1.293,3.382-5.143,7.284-5.143s6.516,3.85,7.284,5.143C15.795,135.06,13.18,138.911,9.278,138.911Z" transform="translate(0 0)"/><path d="M9.751,130.857a3.206,3.206,0,1,0,3.207,3.207A3.21,3.21,0,0,0,9.751,130.857Z" transform="translate(-0.472 -0.295)"/></g></svg>
				</a>

				<div id="connectionInfo" class="hidden">
					<div class="connInfo">
						<div class="textCenter" v-if="!podsReady">
							<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 18"><g transform="translate(0 -183)"><path d="M18.994,201H1.006a1,1,0,0,1-.871-.516,1.052,1.052,0,0,1,0-1.031l8.993-15.974a1.033,1.033,0,0,1,1.744,0l8.993,15.974a1.052,1.052,0,0,1,0,1.031A1,1,0,0,1,18.994,201ZM2.75,198.937h14.5L10,186.059Z"/><rect width="2" height="5.378" rx="0.947" transform="translate(9 189.059)"/><rect width="2" height="2" rx="1" transform="translate(9 195.437)"/></g></svg>
							<h4>Attention</h4>
							<p>
								No pods are available yet for this cluster.<br/>
								You won't be able to connect to it until there's at least one active pod.
							</p>
						</div>
						<template v-else>
							<p>To access StackGres cluster <code>{{ $route.params.namespace + '.' + sgshardedcluster.name }}</code> you can address one or both of the following DNS entries:
								<ul>
									<li>Read Write DNS: <code>{{ sgshardedcluster.data.info.primaryDns }}</code> </li>
									<li>Read Only DNS: <code>{{ sgshardedcluster.data.info.readsDns }}</code> </li>
								</ul>
							</p>	

							<p>You may connect with Postgres client <code>psql</code> in two different ways:
								<ul>
									<li>
										Local <code>psql</code> (runs within the same pod as Postgres):<br/>
										<pre>kubectl -n {{ $route.params.namespace }} exec -ti {{ sgshardedcluster.data.status.clusters[0] }} -c postgres-util -- psql{{ sgshardedcluster.data.info.superuserUsername !== 'postgres' ? ' -U '+ sgshardedcluster.data.info.superuserUsername : '' }}<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
									</li>
									<li>
										Externally to StackGres pods, from a container image that contains <code>psql</code> (this option is the only one available if you have disabled the <code>postgres-util</code> sidecar):<br/>
										<pre>kubectl -n {{ $route.params.namespace }} run psql --rm -it --image ongres/postgres-util --restart=Never -- psql -h {{ sgshardedcluster.data.info.primaryDns }} {{ sgshardedcluster.data.info.superuserUsername }} {{ sgshardedcluster.data.info.superuserUsername }}  <span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
									</li>
								</ul>
							</p>

							<p>The command will ask for the admin user password (prompt may not be shown, just type or paste the password). Use the following command to retrieve it:<br/>
								<pre>kubectl -n {{ $route.params.namespace }} get secret {{ sgshardedcluster.data.info.superuserSecretName }} --template <template v-pre>'{{</template> printf "%s" (index .data "{{ sgshardedcluster.data.info.superuserPasswordKey }}" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
							</p>

							<template v-if="sgshardedcluster.data.spec.postgres.flavor == 'babelfish'">
								<hr/>

								<p>If you wish to connect via the SQL Server protocol, please use the following command:</p>
								<pre>kubectl -n {{ sgshardedcluster.data.metadata.namespace }} run usql --rm -it --image ongres/postgres-util --restart=Never -- usql --password ms://babelfish@{{ sgshardedcluster.data.metadata.name }}:1433<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>

								<br/><br/>
								<p>To retrieve the secrete, please use:</p>
								<pre>kubectl -n {{ sgshardedcluster.data.metadata.namespace }} get secret {{ sgshardedcluster.data.metadata.name }} --template '<template v-pre>{{</template> printf "%s" (index .data "babelfish-password" | base64decode) }}'<span class="copyClipboard" data-tooltip="Copied!" title="Copy to clipboard"></span></pre>
							</template>
						</template>
					</div>
				</div>
			</div>
            <div id="crdDetails">
                <template v-if="hasProp(sgshardedcluster, 'data')">           
                    <div class="configurationDetails">
                        <ShardedClusterSummary :cluster="sgshardedcluster" :extensionsList="extensionsList" :details="true"></ShardedClusterSummary>
                    </div>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
	import store from '@/store'
	import { mixin } from '../../mixins/mixin'
	import sgApi from '../../../api/sgApi'
    import ShardedClusterSummary from '../../forms/summary/SGShardedClusterSummary.vue'

    export default {
        name: 'ShardedClusterConfig',

        components: {
            ShardedClusterSummary
        },

		data() {
			return {
				extensionsList: [],
			}
		},

        mixins: [mixin],

		computed: {
            sgshardedcluster () {
				return store.state.sgshardedclusters.find(c => (c.data.metadata.namespace == this.$route.params.namespace) && (c.data.metadata.name == this.$route.params.name))
			},

			podsReady() {
				return (this.hasPods && this.hasProp(this.sgshardedcluster, 'stats.coordinator.podsReady') && this.sgshardedcluster.stats.coordinator.podsReady && this.hasProp(this.sgshardedcluster, 'stats.shards.podsReady') && this.sgshardedcluster.stats.shards.podsReady)
			}
		},

		created() {
			const vc = this;

			if(!vc.extensionsList.length) {

				sgApi
				.getPostgresExtensions('latest')
				.then(function (response) {
					vc.extensionsList = response.data.extensions
				})
				.catch(function (error) {
					console.log(error.response);
					vc.notify(error.response.data,'error','sgclusters');
				});

			}
		}

	}
</script>