<template>
    <div id="globalDashboard" v-if="loggedIn && isReady && !notFound">
        <header>
            <ul class="breadcrumbs">
                <li class="namespace">
                    Namespaces Overview
                </li>
            </ul>
        </header>

        <div class="content noScroll">
            <h3 class="textCenter pad">Used Namespaces</h3>
            <div class="overview">
                <template v-for="namespace in namespaces" v-if="hasCRDs(namespace)">
                    <div class="card namespace">
                        <table class="fullWidth">
                            <thead>
                                <th class="crdName" colspan="2">
                                    <router-link :to="'/' + namespace" title="Namespace Overview">
                                        <svg xmlns="http://www.w3.org/2000/svg"><g fill="#00adb5"><path d="M1.513.9l-1.5 13a.972.972 0 001 1.1h18a.972.972 0 001-1.1l-1.5-13a1.063 1.063 0 00-1-.9h-15a1.063 1.063 0 00-1 .9zm.6 11.5l.9-8c0-.2.3-.4.5-.4h12.9a.458.458 0 01.5.4l.9 8a.56.56 0 01-.5.6h-14.7a.56.56 0 01-.5-.6zM1.113 17.9a1.063 1.063 0 011-.9h15.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-15.8a1.028 1.028 0 01-1-1.1zM3.113 23h13.8a.972.972 0 001-1.1 1.063 1.063 0 00-1-.9h-13.8a1.063 1.063 0 00-1 .9 1.028 1.028 0 001 1.1zM3.113 25.9a1.063 1.063 0 011-.9h11.8a1.063 1.063 0 011 .9.972.972 0 01-1 1.1h-11.8a1.028 1.028 0 01-1-1.1z"/></g></svg>
                                        <span>{{ namespace }}</span>
                                    </router-link>
                                    <router-link :to="'/' + namespace + '/sgclusters/new'" title="Create Cluster" class="floatRight">
                                        CREATE CLUSTER
                                    </router-link>
                                </th>
                            </thead>
                            <tbody>
                                <tr>
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
                                <tr>
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sginstanceprofiles'" title="Instance Profiles">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g transform="translate(0 -242)"><path d="M19.649,256.971l-1.538-1.3a.992.992,0,1,0-1.282,1.514l.235.2-6.072,2.228v-4.373l.266.154a.974.974,0,0,0,.491.132.99.99,0,0,0,.862-.506,1.012,1.012,0,0,0-.369-1.372l-1.75-1.013a.983.983,0,0,0-.984,0l-1.75,1.013a1.012,1.012,0,0,0-.369,1.372.985.985,0,0,0,1.353.374l.266-.154v4.353l-6.07-2.21.233-.2a.992.992,0,1,0-1.282-1.514l-1.538,1.3a.992.992,0,0,0-.337.925l.342,1.987a.992.992,0,0,0,.977.824.981.981,0,0,0,.169-.015.992.992,0,0,0,.81-1.145l-.052-.3,7.4,2.694A1.011,1.011,0,0,0,10,262c.01,0,.02,0,.03-.005s.02.005.03.005a1,1,0,0,0,.342-.061l7.335-2.691-.051.3a.992.992,0,0,0,.811,1.145.953.953,0,0,0,.168.015.992.992,0,0,0,.977-.824l.341-1.987A.992.992,0,0,0,19.649,256.971Z" fill="#36A8FF"/><path d="M20,246.25a.99.99,0,0,0-.655-.93l-9-3.26a1,1,0,0,0-.681,0l-9,3.26a.99.99,0,0,0-.655.93.9.9,0,0,0,.016.1c0,.031-.016.057-.016.089v5.886a1.052,1.052,0,0,0,.992,1.1,1.052,1.052,0,0,0,.992-1.1v-4.667l7.676,2.779a1.012,1.012,0,0,0,.681,0l7.675-2.779v4.667a1,1,0,1,0,1.984,0v-5.886c0-.032-.014-.058-.016-.089A.9.9,0,0,0,20,246.25Zm-10,2.207L3.9,246.25l6.1-2.206,6.095,2.206Z" fill="#36A8FF"/></g></svg>
                                            <span>SGInstanceProfile <i class="length">{{ profiles.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sginstanceprofiles'" title="Instance Profiles" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr>
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
                                <tr>
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
                                <tr>
                                    <td class="kind">
                                        <router-link :to="'/' + namespace + '/sgbackupconfigs'" title="Managed Backups Configurations">
                                            <svg xmlns="http://www.w3.org/2000/svg" width="26.5" height="18.747" viewBox="0 0 26.5 18.747"><g transform="translate(-60 -212.633)"><path d="M60.955,217.4h10.5a.953.953,0,1,0,0-1.906h-10.5a.953.953,0,1,0,0,1.906Z" fill="#36A8FF"/><path d="M74.795,220.258a.953.953,0,0,0,.955-.953V217.4h4.295a.953.953,0,1,0,0-1.906H75.75v-1.907a.954.954,0,0,0-1.909,0V219.3A.953.953,0,0,0,74.795,220.258Z" fill="#36A8FF"/><path d="M60.955,223.6H65.25V225.5a.954.954,0,0,0,1.909,0v-5.719a.954.954,0,0,0-1.909,0v1.906H60.955a.954.954,0,1,0,0,1.907Z" fill="#36A8FF"/><path d="M67.636,227.884H60.955a.954.954,0,1,0,0,1.907h6.681a.954.954,0,1,0,0-1.907Z" fill="#36A8FF"/><path d="M78.073,222.114h0a.852.852,0,0,0-.668-.293.944.944,0,0,0-.86.667L74.2,227.5l-2.354-5.011a.959.959,0,0,0-.883-.669.834.834,0,0,0-.663.3,1.09,1.09,0,0,0-.238.726v7.568a1.037,1.037,0,0,0,.22.692.776.776,0,0,0,.624.278.787.787,0,0,0,.631-.284,1.038,1.038,0,0,0,.225-.686V226.1l1.568,3.248a1.318,1.318,0,0,0,.355.5.819.819,0,0,0,1.012-.01,1.458,1.458,0,0,0,.35-.486l1.557-3.3v4.361a1.037,1.037,0,0,0,.22.692.776.776,0,0,0,.623.278.823.823,0,0,0,.632-.272,1.009,1.009,0,0,0,.235-.7v-7.568A1.081,1.081,0,0,0,78.073,222.114Z" fill="#36A8FF"/><path d="M86.1,227.268a2.6,2.6,0,0,1,.4,1.469,2.388,2.388,0,0,1-.77,1.885,3.09,3.09,0,0,1-2.12.681H80.531a.7.7,0,0,1-.543-.214.849.849,0,0,1-.2-.6V222.7a.851.851,0,0,1,.2-.6.7.7,0,0,1,.543-.214h2.96a3.041,3.041,0,0,1,2.06.648,2.274,2.274,0,0,1,.746,1.811,2.354,2.354,0,0,1-.352,1.3,2.047,2.047,0,0,1-.973.8A2.038,2.038,0,0,1,86.1,227.268Zm-4.806-1.417h1.947q1.587,0,1.587-1.322a1.2,1.2,0,0,0-.393-.99,1.872,1.872,0,0,0-1.194-.32H81.294Zm3.367,3.782a1.311,1.311,0,0,0,.382-1.042,1.349,1.349,0,0,0-.387-1.056,1.782,1.782,0,0,0-1.213-.347H81.294v2.779h2.149A1.828,1.828,0,0,0,84.661,229.633Z" fill="#36A8FF"/></g></svg>
                                            <span>SGBackupConfig <i class="length">{{ backupconfigs.filter(c => c.data.metadata.namespace == namespace).length }}</i></span>
                                        </router-link>
                                    </td>
                                    <td class="icon invisible">
                                        <router-link :to="'/' + namespace + '/sgbackupconfigs'" title="Managed Backups Configurations" target="_blank"></router-link>
                                    </td>
                                </tr>
                                <tr>
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
                                <tr>
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
                                <tr>
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
            <hr/>
            <div class="textCenter pad">
                <select class="namespaceSelector" v-model="selectedNamespace" @change="goTo(selectedNamespace)">
                    <option disabled selected value="">Or select a namespace...</option>
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
        hasCRDs(namespace) {
            return (
                store.state.clusters.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.profiles.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.pgConfig.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.poolConfig.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.backupConfig.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.logsClusters.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.backups.filter(c => c.data.metadata.namespace == namespace).length ||
                store.state.dbOps.filter(c => c.data.metadata.namespace == namespace).length
            )
        }
    },

    computed: {
        namespaces () {
            return store.state.allNamespaces
        },

        clusters () {
            return store.state.clusters
        },

        profiles () {
            return store.state.profiles
        },

        pgconfigs () {
            return store.state.pgConfig
        },

        poolconfigs () {
            return store.state.poolConfig
        },

        backupconfigs () {
            return store.state.backupConfig
        },

        logsservers () {
            return store.state.logsClusters
        },

        backups () {
            return store.state.backups
        },

        dbops () {
            return store.state.dbOps
        },

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

    .namespaceSelector {
        width: 250px;
        border: 1px solid var(--borderColor);
        background-color: rgba(122, 123, 133, .10);
        color: var(--textColor);
        font-family: 'Poppins', sans-serif;
        text-align: center;
    }
    
</style>
