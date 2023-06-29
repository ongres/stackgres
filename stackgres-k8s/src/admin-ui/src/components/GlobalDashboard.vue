<template>
    <div id="globalDashboard" v-if="loggedIn && isReady && !notFound" :class="!usedNamespaces.length ? 'noCards' : ''">
        <div class="content noScroll">
            <h3 class="textCenter pad">{{ usedNamespaces.length ? 'Used Namespaces' : 'Select a namespace' }}</h3>
            <div class="overview">
                <template v-for="namespace in usedNamespaces">
                    <div class="card namespace">
                        <table class="fullWidth">
                            <thead>
                                <th class="crdName" colspan="2">
                                    <router-link :to="'/' + namespace" title="Namespace Overview">
                                        <svg xmlns="http://www.w3.org/2000/svg"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                                        <span>{{ namespace }}</span>
                                    </router-link>
                                    <router-link 
                                        :to="'/' + namespace + '/sgclusters/new'" 
                                        title="Create Cluster" 
                                        class="floatRight"
                                        v-if="iCan('create', 'sgclusters', namespace)"
                                    >
                                        CREATE CLUSTER
                                    </router-link>
                                </th>
                            </thead>
                            <tbody>
                                <tr v-if="iCan('list', 'sgclusters', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgclusters'" title="Clusters Overview">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"/></svg>
                                            <span>SGCluster <i class="length">{{ clusters.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgclusters'" title="Clusters Overview" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgshardedclusters', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgshardedclusters'" title="Sharded Clusters Overview">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="m19 15.3-1.4-1.2c-.4-.3-1-.3-1.3.1-.3.4-.3 1 .1 1.3h.1l.2.2-5.6 2.1v-4.1l.2.1c.1.1.3.1.5.1.3 0 .6-.2.8-.5.3-.4.1-1-.3-1.3l-1.6-.9c-.3-.2-.6-.2-.9 0l-1.6.9c-.4.3-.6.8-.3 1.3.2.4.8.6 1.2.4l.2-.1v4l-5.6-2.1.2-.2c.4-.3.5-.9.2-1.3s-.9-.5-1.3-.2L1 15.3c-.2.2-.4.5-.3.9L1 18c.1.5.6.8 1.1.8.4-.1.8-.5.8-.9v-.5l6.9 2.5c0 .1.1.1.2.1h.1c.1 0 .2 0 .3-.1l6.8-2.5v.3c-.1.5.3 1 .8 1.1h.2c.4 0 .8-.3.9-.8l.3-1.8c-.1-.3-.2-.7-.4-.9"/><path d="M10 0C4.9 0 .9 2.2.9 5.1v6.3c0 .6.4 1 1 1h.2c.4 0 .8-.3.8-.8.1.1.2.1.4.2h.1c.1 0 .1.1.2.1.1.1.3.1.4.2.1 0 .1.1.2.1s.1 0 .2.1h.1c.1.1.2.1.3.1.1 0 .2.1.3.1.4 0 .8-.3.9-.6 0-.1 0-.1.1-.2.1-.5-.2-.9-.6-1.1-.2-.1-.4-.2-.6-.2-.3-.1-.6-.3-.9-.5-.1-.1-.2-.1-.2-.2l-.1-.1c-.2-.1-.4-.3-.5-.5-.2-.2-.2-.4-.3-.6V8.2c2.1 1.3 4.6 2 7.1 1.9 2.5.1 5-.6 7.1-1.9v.2c0 .2-.1.5-.3.7-.1.2-.3.4-.5.5l-.1.1c-.1.1-.2.1-.3.2-.3.2-.6.3-.9.5-.2.1-.4.2-.6.2-.4.2-.7.6-.6 1.1 0 .1 0 .1.1.2.1.4.5.6.9.6.1 0 .2 0 .4-.1.1-.1.2-.1.4-.1h.1c.1 0 .1-.1.2-.1s.1-.1.2-.1c.1-.1.2-.1.4-.2.1 0 .1-.1.2-.1h.1c.1-.1.3-.1.4-.2 0 .4.3.8.8.8h.2c.6 0 1-.4 1-1V5.1C19.1 2.2 15.1 0 10 0m0 8.1C5.8 8.1 2.9 6.5 2.9 5S5.8 2 10 2s7.1 1.6 7.1 3.1-2.9 3-7.1 3"/></g></svg>
                                            <span>SGShardedCluster <i class="length">{{ sgshardedclusters.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgshardedclusters'" title="Sharded Clusters Overview" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sginstanceprofiles', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sginstanceprofiles'" title="Instance Profiles">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M10 0h.3l8.8 3c.5.2.8.7.6 1.2-.1.3-.3.5-.6.6l-8.8 2.9c-.2.1-.4.1-.6 0L.9 4.9C.4 4.7.1 4.1.3 3.6c.1-.3.3-.5.6-.6L9.7.1c.1-.1.2-.1.3-.1zm5.7 3.9L10 2 4.3 3.9 10 5.8l5.7-1.9zM1.2 6.2c.1 0 .2 0 .3.1l7.3 2.4c.4.1.7.5.7.9V19c0 .5-.4 1-1 1-.1 0-.2 0-.3-.1L.9 17.5c-.4-.1-.7-.5-.7-.9V7.2c0-.6.4-1 1-1zm6.2 4.1L2.1 8.6v7.3l5.3 1.8v-7.4zM18.8 6.2c.5 0 1 .4 1 1v9.4c0 .4-.3.8-.7.9l-7.3 2.4c-.5.2-1.1-.1-1.2-.6 0-.1-.1-.2-.1-.3V9.6c0-.4.3-.8.7-.9l7.3-2.4c.1-.1.2-.1.3-.1zm-.9 9.7V8.6l-5.3 1.8v7.3l5.3-1.8z"/></g></svg>
                                            <span>SGInstanceProfile <i class="length">{{ profiles.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sginstanceprofiles'" title="Instance Profiles" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgpgconfigs', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgpgconfigs'" title="Postgres Configurations">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z" class="a"></path></svg>
                                            <span>SGPostgresConfig <i class="length">{{ pgconfigs.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgpgconfigs'" title="Postgres Configurations" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgpoolconfigs', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgpoolconfigs'" title="Connection Pooling Configurations">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>
                                            <span>SGPoolingConfig <i class="length">{{ poolconfigs.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgpoolconfigs'" title="Connection Pooling Configurations" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgobjectstorages', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgobjectstorages'" title="Object Storage Configurations">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 18.8"><g fill="#36A8FF"><path d="M1 4.8h10.5c.5 0 1-.4 1-1s-.4-1-1-1H1c-.5 0-1 .4-1 1s.5 1 1 1zM14.8 7.6c.5 0 1-.4 1-1V4.8h4.3c.5 0 1-.4 1-1s-.4-1-1-1h-4.3V1c0-.5-.4-1-1-1s-1 .4-1 1v5.7c.1.5.5.9 1 .9zM1 11h4.3v1.9c0 .5.4 1 1 1s1-.4 1-1V7.1c0-.5-.4-1-1-1s-1 .4-1 1V9H1c-.5 0-1 .5-1 1s.4.9 1 1c-.1 0 0 0 0 0zM7.7 15.3H1c-.5 0-1 .4-1 .9s.4 1 .9 1h6.8c.5 0 1-.4 1-.9 0-.6-.4-1-1-1z"/><g><path d="M14.275 18.7c-.8.1-1.6-.1-2.3-.6-.7-.4-1.2-1-1.5-1.7-.4-.8-.6-1.6-.6-2.5 0-.9.2-1.8.5-2.6.3-.7.9-1.3 1.5-1.7.7-.4 1.5-.6 2.3-.6.8 0 1.6.2 2.3.6.7.4 1.2 1 1.5 1.7.5.8.7 1.7.7 2.6 0 .9-.2 1.8-.5 2.6-.4.7-.9 1.2-1.6 1.6-.7.5-1.5.7-2.3.6zm0-1.6c.7 0 1.4-.3 1.8-.8.5-.7.7-1.6.6-2.4.1-.9-.2-1.7-.6-2.4-.5-.6-1.1-.9-1.8-.9s-1.4.3-1.8.9c-.4.7-.7 1.5-.6 2.4-.1.8.2 1.7.6 2.4.4.5 1.1.8 1.8.8zM22.875 18.7c-.6 0-1.3-.1-1.9-.2-.5-.1-1-.4-1.4-.7 0-.1-.1-.2-.2-.3-.1-.2-.1-.3-.1-.5s.1-.4.2-.6c.1-.2.3-.2.4-.3h.3c.1 0 .2.1.3.2.3.2.7.4 1.1.5.5.3.9.3 1.3.3s.9-.1 1.3-.3c.3-.2.5-.5.4-.9 0-.3-.2-.5-.4-.7-.5-.2-1-.4-1.5-.5-.6-.1-1.3-.3-1.9-.6-.4-.2-.8-.5-1-.9-.2-.3-.3-.8-.3-1.2 0-.5.2-1.1.5-1.5.3-.5.8-.8 1.3-1.1.6-.3 1.2-.4 1.8-.4 1.1 0 2.1.3 3 1l.3.3c.1.1.1.3.1.4 0 .2-.1.4-.2.6-.1.2-.3.2-.4.3h-.3c-.1 0-.2-.1-.3-.2-.3-.2-.6-.4-1-.5-.4-.1-.7-.2-1.1-.2-.4 0-.9.1-1.2.3-.3.2-.5.5-.4.9 0 .2.1.4.2.5.2.2.4.3.6.4.1.1.5.2.9.3.9.2 1.7.5 2.5 1 .5.4.8 1.1.8 1.7 0 .5-.1 1.1-.4 1.5-.3.5-.8.8-1.3 1-.7.3-1.3.5-2 .4z"/></g></g></svg>
                                            <span>SGObjectStorage <i class="length">{{ objectStorages.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgobjectstorages'" title="Object Storage Configurations" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgscripts', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgscripts'" title="Scripts Configurations">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="25.628" height="18.617" viewBox="0 0 25.628 18.617"><path d="M-96.745,34.762h10.5a.954.954,0,0,0,.954-.953.953.953,0,0,0-.954-.952h-10.5a.953.953,0,0,0-.955.952A.954.954,0,0,0-96.745,34.762Z" transform="translate(97.7 -30)"/><path d="M-82.9,37.619a.954.954,0,0,0,.955-.952v-1.9h4.295a.954.954,0,0,0,.955-.953.953.953,0,0,0-.955-.952H-81.95v-1.9A.953.953,0,0,0-82.9,30a.953.953,0,0,0-.954.952v5.715A.953.953,0,0,0-82.9,37.619Z" transform="translate(97.7 -30)"/><path d="M-96.745,40.952h4.3v1.9a.953.953,0,0,0,.955.953.953.953,0,0,0,.954-.953V37.143a.954.954,0,0,0-.954-.953.954.954,0,0,0-.955.953v1.9h-4.3A.954.954,0,0,0-97.7,40,.953.953,0,0,0-96.745,40.952Z" transform="translate(97.7 -30)"/><path d="M-90.064,45.238h-6.681a.953.953,0,0,0-.955.953.953.953,0,0,0,.955.952h6.681a.953.953,0,0,0,.955-.952A.953.953,0,0,0-90.064,45.238Z" transform="translate(97.7 -30)"/><path d="M4.16.117A6.564,6.564,0,0,1,2.269-.149,4.164,4.164,0,0,1,.819-.871.73.73,0,0,1,.52-1.5.733.733,0,0,1,.683-1.97a.481.481,0,0,1,.383-.2,1.056,1.056,0,0,1,.533.182,3.9,3.9,0,0,0,1.17.585,4.724,4.724,0,0,0,1.352.182,2.662,2.662,0,0,0,1.456-.331,1.085,1.085,0,0,0,.507-.969.833.833,0,0,0-.475-.76A6.283,6.283,0,0,0,4.03-3.8a9.356,9.356,0,0,1-1.859-.572A2.625,2.625,0,0,1,1.092-5.2,2.152,2.152,0,0,1,.728-6.487,2.422,2.422,0,0,1,1.183-7.93a3,3,0,0,1,1.268-.995,4.49,4.49,0,0,1,1.826-.357,4.534,4.534,0,0,1,3.029.988.976.976,0,0,1,.241.28.738.738,0,0,1,.072.345.733.733,0,0,1-.163.474.481.481,0,0,1-.383.2.718.718,0,0,1-.241-.039,1.927,1.927,0,0,1-.292-.143,4.392,4.392,0,0,0-1.034-.579,3.5,3.5,0,0,0-1.228-.188,2.409,2.409,0,0,0-1.4.357,1.145,1.145,0,0,0-.513.994.912.912,0,0,0,.455.819,5.333,5.333,0,0,0,1.547.533,10.409,10.409,0,0,1,1.878.578,2.786,2.786,0,0,1,1.105.812,1.953,1.953,0,0,1,.384,1.235,2.359,2.359,0,0,1-.449,1.424,2.923,2.923,0,0,1-1.261.962A4.743,4.743,0,0,1,4.16.117Zm9.217,0a4.572,4.572,0,0,1-2.321-.572A3.8,3.8,0,0,1,9.529-2.093,5.516,5.516,0,0,1,9-4.589a5.475,5.475,0,0,1,.533-2.49A3.811,3.811,0,0,1,11.056-8.71a4.572,4.572,0,0,1,2.321-.572,5.026,5.026,0,0,1,1.612.253,4.188,4.188,0,0,1,1.339.734.746.746,0,0,1,.234.273.846.846,0,0,1,.065.351.746.746,0,0,1-.156.481.468.468,0,0,1-.377.195.961.961,0,0,1-.533-.182,3.855,3.855,0,0,0-1.04-.579,3.315,3.315,0,0,0-1.066-.163,2.5,2.5,0,0,0-2.048.845,3.829,3.829,0,0,0-.7,2.483,3.852,3.852,0,0,0,.7,2.5,2.5,2.5,0,0,0,2.048.845,3.06,3.06,0,0,0,1.033-.169,4.958,4.958,0,0,0,1.073-.572,1.819,1.819,0,0,1,.26-.13.728.728,0,0,1,.273-.052.468.468,0,0,1,.377.2.746.746,0,0,1,.156.481.857.857,0,0,1-.065.344.728.728,0,0,1-.234.28,4.188,4.188,0,0,1-1.339.735A5.026,5.026,0,0,1,13.377.117Z" transform="translate(9.001 18.5)"/></svg>
                                            <span>SGScripts <i class="length">{{ sgscripts.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgscripts'" title="Scripts Configurations" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgdistributedlogs', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgdistributedlogs'" title="Distributed Logs Servers">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>
                                            <span>SGDistributedLogs <i class="length">{{ logsservers.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgdistributedlogs'" title="Distributed Logs Servers" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgbackups', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgbackups'" title="Cluster Backups">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>
                                            <span>SGBackups <i class="length">{{ backups.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgbackups'" title="Cluster Backups" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr v-if="iCan('list', 'sgdbops', namespace)">
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgdbops'" title="Databse Operations">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M17.1 20c-.6 0-1-.5-1-1 0-1.6-1.3-2.8-2.8-2.8H6.6c-1.6 0-2.8 1.3-2.8 2.8 0 .6-.5 1-1 1s-1-.5-1-1c0-2.7 2.2-4.8 4.8-4.8h6.7c2.7 0 4.8 2.2 4.8 4.8.1.5-.4 1-1 1zM9.9 9.4c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5c.1 1.4-1.1 2.5-2.5 2.5zm0-3.3c-.4 0-.8.3-.8.8 0 .4.3.8.8.8.5-.1.8-.4.8-.8 0-.5-.3-.8-.8-.8z"/><path d="M10 13.7h-.2c-1-.1-1.8-.8-1.8-1.8v-.1h-.1l-.1.1c-.8.7-2.1.6-2.8-.2s-.7-1.9 0-2.6l.1-.1H5c-1.1 0-2-.8-2.1-1.9 0-1.2.8-2.1 1.8-2.2H5v-.1c-.7-.8-.7-2 .1-2.8.8-.7 1.9-.7 2.7 0 .1 0 .1 0 .2-.1 0-.6.3-1.1.7-1.4.8-.7 2.1-.6 2.8.2.2.3.4.7.4 1.1v.1h.1c.8-.7 2.1-.6 2.8.2.6.7.6 1.9 0 2.6l-.1.1v.1h.1c.5 0 1 .1 1.4.5.8.7.9 2 .2 2.8-.3.4-.8.6-1.4.7h-.3c.4.4.6 1 .6 1.5-.1 1.1-1 1.9-2.1 1.9-.4 0-.9-.2-1.2-.5l-.1-.1v.1c0 1.1-.9 1.9-1.9 1.9zM7.9 10c1 0 1.8.8 1.8 1.7 0 .1.1.2.2.2s.2-.1.2-.2c0-1 .8-1.8 1.8-1.8.5 0 .9.2 1.3.5.1.1.2.1.3 0s.1-.2 0-.3c-.7-.7-.7-1.8 0-2.5.3-.3.8-.5 1.3-.5h.1c.1 0 .2 0 .2-.1 0 0 .1-.1.1-.2s0-.1-.1-.2c0 0-.1-.1-.2-.1h-.2c-.7 0-1.4-.4-1.6-1.1 0-.1 0-.1-.1-.2-.2-.6-.1-1.3.4-1.8.1-.1.1-.2 0-.3s-.2-.1-.3 0c-.3.3-.8.5-1.2.5-1 0-1.8-.8-1.8-1.8 0-.1-.1-.2-.2-.2s-.1 0-.2.1c.1.1 0 .2 0 .3 0 .7-.4 1.4-1.1 1.7-.1 0-.1 0-.2.1-.6.2-1.3 0-1.8-.4-.1-.1-.2-.1-.3 0-.1.1-.1.2 0 .3.3.3.5.7.5 1.2.1 1-.7 1.9-1.7 1.9h-.2c-.1 0-.1 0-.2.1 0-.1 0 0 0 0 0 .1.1.2.2.2h.2c1 0 1.8.8 1.8 1.8 0 .5-.2.9-.5 1.2-.1.1-.1.2 0 .3s.2.1.3 0c.3-.2.7-.4 1.1-.4h.1z"/></g></svg>
                                            <span>SGDbOps <i class="length">{{ dbops.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgdbops'" title="Databse Operations" target="_blank"></router-link>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </template>
            </div>
            <hr v-if="usedNamespaces.length"/>
            <div class="textCenter pad">
                <select class="plain" v-model="selectedNamespace" @change="goTo(selectedNamespace)">
                    <option disabled selected value="">{{ usedNamespaces.length ? 'Or select a namespace...' : 'Namespaces' }}</option>
                    <option v-for="namespace in namespaces" :value="'/' + namespace">
                        {{ namespace }}
                    </option>
                </select>
            </div>
        </div>
    </div>
</template>

<script>
    import store from '../store'
	import { mixin } from './mixins/mixin'

export default {
    name: 'NamespaceOverview',
    
    mixins: [mixin],
    
    data() {
        return {
            selectedNamespace: '' 
        }
    },

    methods: {
        
    },

    computed: {
        namespaces () {
            return store.state.allNamespaces
        },

        clusters () {
            return store.state.sgclusters
        },

        sgshardedclusters () {
            return store.state.sgshardedclusters
        },

        profiles () {
            return store.state.sginstanceprofiles
        },

        pgconfigs () {
            return store.state.sgpgconfigs
        },

        poolconfigs () {
            return store.state.sgpoolconfigs
        },

        objectStorages () {
            return store.state.sgobjectstorages
        },

        sgscripts () {
            return store.state.sgscripts
        },

        logsservers () {
            return store.state.sgdistributedlogs
        },

        backups () {
            return store.state.sgbackups
        },

        dbops () {
            return store.state.sgdbops
        },

        usedNamespaces() {
            const vc = this;
            const usedNamespaces = [];

            store.state.namespaces.forEach(function(namespace) {
                const namespaceHasCrd = store.state.sgclusters.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sginstanceprofiles.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgpgconfigs.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgpoolconfigs.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgdistributedlogs.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgbackups.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgdbops.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgobjectstorages.filter(c => c.data.metadata.namespace == namespace).length ||
                    store.state.sgscripts.filter(c => c.data.metadata.namespace == namespace).length;

                if(namespaceHasCrd)
                    usedNamespaces.push(namespace)

            })

            if(!usedNamespaces.length)
                $('#header').addClass('hide')
            else
                $('#header').removeClass('hide')
                
            return usedNamespaces
        }

    }

}
</script>

<style scoped>

    .card.namespace {
        height: auto;
        max-height: none;
    }

    .card.namespace th a {
        display: inline-block;
    }

    .card.namespace a.floatRight {
        color: var(--blue);
    }

    .card.namespace a.floatRight:hover {
        color: var(--lBlue);
    }

    .card.namespace .crdName svg {
        transform: scale(.7);
        position: relative;
        width: 15px;
        top: -4px;
    }

    td.kind svg {
        height: 15px;
        width: auto;
        position: relative;
        top: 2px;
        margin-left: 35px;
        margin-right: 7px;
    }

    i.length {
        margin-top: 2px;
    }

    thead .icon svg {
        transform: rotate(-45deg)
    }

    .noCards h3 {
        margin-bottom: -10px;
    }
    
</style>
