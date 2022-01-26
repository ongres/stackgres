import store from '../../store'
import axios from 'axios'
import router from '../../router'
import VueMarkdown from 'vue-markdown'
import moment from 'moment'

export const mixin = {

    data: function(){
      return {
        confirmDeleteName: '',
        pagination: {
          amount: 0,
          rows: 999,
          start: 0,
          end: 999,
          current: this.$route.query.hasOwnProperty('page') ? parseInt(this.$route.query.page) : 1,
          singleLoaded: false
        }
      }
    },
    components: {
      VueMarkdown
    },
    computed: {
  
      loggedIn () {
        if (store.state.hasOwnProperty('loginToken'))
          return store.state.loginToken.length > 0
        else
          return false
      },

      notFound () {
        return store.state.notFound
      },

      isReady () {
        return store.state.ready
      },

      tooltips () {
        return store.state.tooltips
      }
  
    },
    methods: {
      
      cancel: function() {
        const vc = this
        
        if(window.history.length > 2)
          vc.$router.go(-1)
        else
          vc.$router.push('/default/sgclusters')
      },

      checkAuthError: function(error) {
        if(error.response) {
          if(error.response.status == 401 ) {
            document.cookie = 'sgToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT; SameSite=Strict;';
            store.commit('setLoginToken');
            window.location.replace('/admin/')
            process.exit(1);
          } else if(error.response.status == 403 ) {
            
            // Little hack to store the right plural kind to validate RBAC permissions
            if(error.response.config.url.includes('sgcluster/stats'))
              kind = 'stats'
            else
              kind = error.response.config.url.replace('/stackgres/','')+'s';
            
            store.commit('setNoPermissions', kind);
          }
        }
      },      

      fetchAPI  (kind = '') {

        const vc = this

        let loginToken = ("; "+document.cookie).split("; sgToken=").pop().split(";").shift();
  
        if(store.state.loginToken.search('Authentication Error') !== -1) {
          clearInterval(this.pooling);
        } else if (!loginToken.length) {
          if(!store.state.loginToken.length) {
            $('#signup').addClass('login').fadeIn();
            return false;
          }
        } else if ( !store.state.loginToken.length && (loginToken.length > 0) ) {
          $('#signup').hide();
          store.commit('setLoginToken', loginToken);
        }
  
        $('#reload').addClass('active');
  
        // Read and set user permissions
        axios
        .get('/stackgres/auth/rbac/can-i')
        .then( function(response) {
          //console.log(response.data)
          store.commit('setPermissions', response.data);
        }).catch(function(err) {
          console.log(err);
          vc.checkAuthError(err);
        });

        if ( !store.state.permissions.forbidden.includes('namespaces') && ( !kind.length || (kind == 'namespaces') ) ) {
          /* Namespaces Data */
          axios
          .get('/stackgres/namespaces')
          .then( function(response){
  
            if(vc.$route.params.hasOwnProperty('namespace') && !response.data.includes(vc.$route.params.namespace)) {
              router.push('/')
              vc.notify('The namespace you were browsing has been deleted from the server')
            }
            store.commit('addNamespaces', response.data);

          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if ( !store.state.permissions.forbidden.includes('sgclusters') && ( !kind.length || (kind == 'sgclusters') ) ){
          /* Clusters Data */
          axios
          .get('/stackgres/sgclusters')
          .then( function(response){

            vc.lookupCRDs('sgcluster', response.data);
  
            response.data.forEach( function(item, index) {

              var cluster = {
                name: item.metadata.name,
                data: item,
                hasBackups: false,
                status: {}
              };
              
              if(!store.state.namespaces.includes(item.metadata.namespace))
                store.commit('updateNamespaces', item.metadata.namespace);

              store.commit('updateClusters', cluster);

              axios
              .get('/stackgres/namespaces/'+cluster.data.metadata.namespace+'/sgclusters/'+cluster.data.metadata.name+'/stats')
              .then( function(resp) {
                store.commit('updateClusterStats', {
                  name: cluster.data.metadata.name,
                  namespace: cluster.data.metadata.namespace,
                  stats: resp.data
                })
              }).catch(function(err) {
                console.log(err);
              });

              // Set as current cluster if no other cluster has already been set
              if(!store.state.currentCluster)              
                store.commit('setCurrentCluster', cluster);

            });
            
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
  
        }
  
        if (!store.state.permissions.forbidden.includes('sgbackups') && ( !kind.length || (kind == 'sgbackups') )) {
          
          /* Backups */
          axios
          .get('/stackgres/sgbackups')
          .then( function(response) {

            vc.lookupCRDs('sgbackup', response.data);
  
              var start, finish, duration;
  
              response.data.forEach( function(item, index) {
                
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
  
                //console.log(item);
                if( (item.status !== null) && item.status.hasOwnProperty('process')) {
                  if( item.status.process.status === 'Completed' ) {
                    //console.log('setting duration');
                    start = moment(item.status.process.timing.start);
                    finish = moment(item.status.process.timing.stored);
                    duration = new Date(moment.duration(finish.diff(start))).toISOString();
                  } else {
                    duration = '';
                  }
                  
                }

                if(!index)
                  store.commit('flushBackups')
                  
                store.commit('updateBackups', { 
                  name: item.metadata.name,
                  data: item,
                  duration: duration,
                  show: true
                });
  
              });
  
              store.state.clusters.forEach(function(cluster, index){
                let backups = store.state.backups.find(b => ( (cluster.name == b.data.spec.sgCluster) && (cluster.data.metadata.namespace == b.data.metadata.namespace) ) );
        
                if ( typeof backups !== "undefined" )
                  cluster.hasBackups = true; // Enable/Disable Backups button
  
              });
  
              
  
              //console.log("Backups Data updated");
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if ( !store.state.permissions.forbidden.includes('sgpgconfigs') && (!kind.length || (kind == 'sgpgconfigs') ) ){
  
          /* PostgreSQL Config */
          axios
          .get('/stackgres/sgpgconfigs')
          .then( function(response) {

            vc.lookupCRDs('sgpgconfig', response.data);
  
            response.data.forEach( function(item, index) {
                
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                store.commit('updateNamespaces', item.metadata.namespace);
              
              if(!index)
                store.commit('flushPGConfig')
                
              store.commit('updatePGConfig', { 
                name: item.metadata.name,
                data: item
              });

            });

            // console.log("PGconf Data updated");
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if (!store.state.permissions.forbidden.includes('sgpoolconfigs') && ( !kind.length || (kind == 'sgpoolconfig') ) ){
  
          /* Connection Pooling Config */
          axios
          .get('/stackgres/sgpoolconfigs')
          .then( function(response) {

            vc.lookupCRDs('sgpoolconfig', response.data);
  
            response.data.forEach( function(item, index) {
                
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
                
                if(!index)
                  store.commit('flushPoolConfig')
                  
                store.commit('updatePoolConfig', { 
                  name: item.metadata.name,
                  data: item
                });
  
              });
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if ( !store.state.permissions.forbidden.includes('sgbackupconfigs') && ( !kind.length || (kind == 'sgbackupconfigs')) ) {
  
          /* Backup Config */
          axios
          .get('/stackgres/sgbackupconfigs')
          .then( function(response) {

            vc.lookupCRDs('sgbackupconfig', response.data);
  
              response.data.forEach( function(item, index) {
                
                if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                  store.commit('updateNamespaces', item.metadata.namespace);
                
                if(!index)
                  store.commit('flushBackupConfig')
                  
                store.commit('updateBackupConfig', { 
                  name: item.metadata.name,
                  data: item
                });
  
              });
  
              // console.log("BackupConfig Data updated");
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if ( !store.state.permissions.forbidden.includes('sginstanceprofiles') && (!kind.length || (kind == 'sginstanceprofiles') ) ) {
  
          /* Profiles */
          axios
          .get('/stackgres/sginstanceprofiles')
          .then( function(response) {

            vc.lookupCRDs('sginstanceprofile', response.data);
  
            response.data.forEach( function(item, index) {
                
              if(store.state.namespaces.indexOf(item.metadata.namespace) === -1)
                store.commit('updateNamespaces', item.metadata.namespace);
              
              if(!index)
                store.commit('flushProfiles')

              store.commit('updateProfiles', { 
                name: item.metadata.name,
                data: item
              });
  
            });
  
              // console.log("Profiles Data updated");
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if (!store.state.permissions.forbidden.includes('storageclasss') && ( !kind.length || (kind == 'storageclasses') )) {
          /* Storage Classes Data */
          axios
          .get('/stackgres/storageclasses',
            { headers: {
                //'content-type': 'application/json'
              }
            }
          )
          .then( function(response){
  
            store.commit('addStorageClasses', response.data);
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
        if (!store.state.permissions.forbidden.includes('sgdistributedlogs') && ( !kind.length || (kind == 'sgdistributedlogs') ) ){
          /* Distribude Logs Data */
          axios
          .get('/stackgres/sgdistributedlogs',
            { headers: {
                //'content-type': 'application/json'
              }
            }
          )
          .then( function(response){

            vc.lookupCRDs('sgdistributedlogs', response.data);
  
            var logs = [];
  
            response.data.forEach(function(item, index){
              logs.push({
                name: item.metadata.name,
                data: item
              })
            })

            store.commit('addLogsClusters', logs);
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }

        if (!store.state.permissions.forbidden.includes('sgdbops') && ( !kind.length || (kind == 'sgdbops') ) ){
          /* DbOps Data */
          axios
          .get('/stackgres/sgdbops',
            { headers: {
                //'content-type': 'application/json'
              }
            }
          )
          .then( function(response){

            vc.lookupCRDs('sgdbops', response.data);
  
            var dbOps = [];
  
            response.data.forEach(function(item, index){
              dbOps.push({
                name: item.metadata.name,
                data: item
              })
            })

            store.commit('addDbOps', dbOps);
  
          }).catch(function(err) {
            console.log(err);
            vc.checkAuthError(err);
          });
        }
  
  
        if (!store.state.permissions.forbidden.includes('sgbackups') && ( (kind === 'sgbackups') || (kind === 'sgclusters') ) ) {
          // Check if current cluster has backups
          let currentClusterBackups = store.state.backups.find(b => ( (store.state.currentCluster.name == b.data.spec.cluster) && (store.state.currentCluster.data.metadata.namespace == b.data.metadata.namespace) ) );
            
          if ( typeof currentClusterBackups !== "undefined" )
            store.state.currentCluster.hasBackups = true; // Enable/Disable Backups button
        }

        if(!Object.keys(store.state.tooltips).length && !store.state.tooltips.hasOwnProperty('error')) {
          fetch('/admin/info/sg-tooltips.json')
          .then(response => response.json())
          .then(data => {
            var tooltips = data.components.schemas;
            vc.cleanupTooltips(tooltips)
            store.commit('setTooltips', tooltips)
          })
          .catch((error) => {
            console.log(error);
            
            $('body').addClass('noTooltips')
            
            vc.notify({
              title: 'INFO', 
              detail: 'Information tooltips could not be loaded properly, help texts won\'t be available. <br/><br/>This does not affect the usability of the web console. You can always refer to the <a href="https://stackgres.io/doc" target="_blank">official StackGres Documentation</a> for more information.',
              type: 'https://stackgres.io/doc'
            }, 'error')

            store.commit('setTooltips', {error: 'Information tooltips could not be loaded'})
          });
        }

        if(!Object.keys(store.state.postgresVersions).length) {

          let pgFlavors = {'vanilla': {}, 'babelfish': {}};

          Object.keys(pgFlavors).forEach(function(flavor) {
            /* Postgres versions */
            axios
            .get('/stackgres/version/postgresql?flavor=' + flavor)
            .then( function(response){

              let postgresVersions = {};

              response.data.postgresql.forEach(function(version) {
                let major = version.split('.')[0];
                
                if(!postgresVersions.hasOwnProperty(major)) {
                  postgresVersions[major] = [];
                }
                
                postgresVersions[major].push(version);
              })
  
              pgFlavors[flavor] = postgresVersions;
    
            }).catch(function(err) {
              console.log(err);
              vc.checkAuthError(err);
            });
          });

          store.commit('setPostgresVersions', pgFlavors);

        }

        if(!store.state.ready)
          store.commit('setReady',true)
  
        setTimeout(function(){
          $("#reload").removeClass("active");
          this.init = true;
        }, 2000);

      },
  
      setContentTooltip( el = '', warning = false ) {
        if(warning) {
          $('.contentTooltip .info').addClass('warning')
        } else {
          $('.contentTooltip .info').removeClass('warning');
        }

        if(el.length) {
          $('.contentTooltip .info .content').html($(el).html());
          $('.contentTooltip').addClass('show');
        } else {
          $('.contentTooltip .info .content').html('');
          $('.contentTooltip').removeClass('show');
        }
      },
  
      helpTooltip(kind, field) {
        const crd = store.state.tooltips[kind];
        let param = crd;
  
        if(field == 'spec.postgresql.conf') {
          param =  crd.spec['postgresql.conf']
        } else if (field == 'spec.pgBouncer.pgbouncer.ini') {
          param =  crd.spec.pgBouncer['pgbouncer.ini']
        } else {
          params = field.split('.');
          params.forEach(function(item, index){
            if( !index ) // First level
              param = param[item]
            else if (param.type == 'object')
              param = param.properties[item]
            else if (param.type == 'array')
              param = param.items.properties[item]
          })
        }
        
        store.commit('setTooltipsText', param.description)
        $('#helpTooltip').show()
      },
  
      hasProp (obj, propertyPath) {
        if(!propertyPath)
            return false;
  
        var properties = propertyPath.split('.');
  
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
  
            if(!obj || !obj.hasOwnProperty(prop)){
                return false;
            } else {
                obj = obj[prop];
            }
        }
  
        return true;
      },
  
      iCan( action, kind, namespace = '' ) {
        
        if(namespace.length) {
          var iCan = false;
          
          store.state.permissions.allowed.namespaced.forEach(function( ns ){
            if( (ns.namespace == namespace) && ns.resources.hasOwnProperty(kind) && (ns.resources[kind].includes(action)) ) {
              iCan = true;
              return false
            }
          });
          
          return iCan
        } else {
          return !store.state.permissions.forbidden.includes(kind)
        }
  
      },
  
      iCant ( ) {
        return "<p>You don't have enough permissions to access this resource</p>"
      },
  
      sort: function(param, type = 'alphabetical') {
              
        if(param === this.currentSort.param) {
          this.currentSortDir = this.currentSortDir==='asc'?'desc':'asc';
        }
        this.currentSort.param = param;
        this.currentSort.type = type;

      },
  
      deleteCRD: function( kind, namespace, name, redirect ) {
  
        //console.log("Open delete");
        //$('#delete input').val('');
        this.confirmDeleteName = '';
        $("#delete").addClass("active");
        $(".filter > .open").removeClass("open");
  
        store.commit("setDeleteItem", {
          kind: kind,
          namespace: namespace,
          name: name,
          redirect: redirect
        });
  
      },

      notify (message, kind = 'message', crd = 'general', singleTooltip = true) {
      
        $("#delete").removeClass("active");

        if(singleTooltip) {
          $("#notifications.hasTooltip.active").removeClass("active");
          $("#notifications.hasTooltip .message.show").removeClass("show");
        }
      
        let details = '';
        let icon = '';
      
        $(".form .alert").removeClass("alert");
      
        switch (crd) {
          case 'general':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M13.193 10A3.193 3.193 0 1010 13.2a3.2 3.2 0 003.193-3.2zm-1.809 0A1.384 1.384 0 1110 8.614 1.386 1.386 0 0111.384 10z"/><path class="a" d="M16.961 12.835a.443.443 0 01.44-.246 2.6 2.6 0 000-5.2h-.136a.4.4 0 01-.318-.157.988.988 0 00-.055-.164.427.427 0 01.122-.486A2.6 2.6 0 1013.3 2.937a.414.414 0 01-.287.116.4.4 0 01-.292-.12.455.455 0 01-.123-.357 2.591 2.591 0 00-.762-1.84 2.659 2.659 0 00-3.675 0 2.6 2.6 0 00-.76 1.84v.137a.406.406 0 01-.158.318 1.078 1.078 0 00-.163.055.41.41 0 01-.465-.1l-.076-.077a2.5 2.5 0 00-1.853-.729 2.576 2.576 0 00-1.822.8 2.632 2.632 0 00.1 3.71.434.434 0 01.058.5.423.423 0 01-.422.265 2.6 2.6 0 000 5.2h.133a.41.41 0 01.285.117.43.43 0 01-.035.629l-.079.079v.005A2.61 2.61 0 003 17.135a2.479 2.479 0 001.853.728 2.614 2.614 0 001.847-.827.429.429 0 01.5-.057.419.419 0 01.264.42 2.6 2.6 0 105.2 0v-.132a.414.414 0 01.116-.284.421.421 0 01.3-.126.356.356 0 01.278.113l.1.1a2.731 2.731 0 001.852.728 2.6 2.6 0 002.55-2.65 2.611 2.611 0 00-.825-1.857.4.4 0 01-.081-.444zm-6.2 4.422v.143a.691.691 0 01-.69.691.718.718 0 01-.692-.788 2.289 2.289 0 00-1.457-2.095 2.274 2.274 0 00-.919-.2 2.427 2.427 0 00-1.7.728.7.7 0 01-.5.213.652.652 0 01-.482-.194.676.676 0 01-.208-.477.749.749 0 01.217-.53l.064-.064a2.323 2.323 0 00-1.654-3.938H2.6a.692.692 0 01-.489-1.18.755.755 0 01.587-.2A2.286 2.286 0 004.788 7.9a2.306 2.306 0 00-.467-2.556l-.069-.069a.693.693 0 01.478-1.191.655.655 0 01.5.213l.069.071a2.257 2.257 0 002.334.536.92.92 0 00.27-.071 2.312 2.312 0 001.4-2.121v-.134a.687.687 0 01.2-.489.705.705 0 01.977 0 .751.751 0 01.2.571 2.3 2.3 0 00.705 1.64 2.331 2.331 0 001.649.665 2.369 2.369 0 001.652-.713.691.691 0 011.181.488.753.753 0 01-.259.547 2.253 2.253 0 00-.538 2.334.932.932 0 00.072.274 2.313 2.313 0 002.119 1.4h.139a.691.691 0 01.69.692.717.717 0 01-.768.691 2.312 2.312 0 00-2.113 1.395 2.345 2.345 0 00.533 2.619.693.693 0 01-.45 1.192.749.749 0 01-.506-.19l-.1-.1a2.4 2.4 0 00-1.653-.654 2.325 2.325 0 00-2.283 2.312zM5.5 4.177z"/></svg>';
            break;
          case 'sgclusters':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10 0C4.9 0 .9 2.218.9 5.05v11.49C.9 19.272 6.621 20 10 20s9.1-.728 9.1-3.46V5.05C19.1 2.218 15.1 0 10 0zm7.1 11.907c0 1.444-2.917 3.052-7.1 3.052s-7.1-1.608-7.1-3.052v-.375a12.883 12.883 0 007.1 1.823 12.891 12.891 0 007.1-1.824zm0-3.6c0 1.443-2.917 3.052-7.1 3.052s-7.1-1.61-7.1-3.053v-.068A12.806 12.806 0 0010 10.1a12.794 12.794 0 007.1-1.862zM10 8.1c-4.185 0-7.1-1.607-7.1-3.05S5.815 2 10 2s7.1 1.608 7.1 3.051S14.185 8.1 10 8.1zm-7.1 8.44v-1.407a12.89 12.89 0 007.1 1.823 12.874 12.874 0 007.106-1.827l.006 1.345C16.956 16.894 14.531 18 10 18c-4.822 0-6.99-1.191-7.1-1.46z"></path></svg>';
            break;
          case 'sgpgconfigs':
          case 'sgpostgresconfigs':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.7 20"><path d="M10.946 18.7a.841.841 0 01-.622-.234.862.862 0 01-.234-.635v-7.817a.8.8 0 01.221-.6.834.834 0 01.608-.214h3.29a3.4 3.4 0 012.353.755 2.7 2.7 0 01.843 2.12 2.72 2.72 0 01-.843 2.126 3.379 3.379 0 01-2.353.764h-2.394v2.875a.8.8 0 01-.869.867zM14 13.637q1.778 0 1.778-1.551T14 10.535h-2.18v3.1zm11.968-.107a.683.683 0 01.494.181.625.625 0 01.191.477v2.875a1.717 1.717 0 01-.16.87 1.174 1.174 0 01-.655.414 6.882 6.882 0 01-1.242.294 9.023 9.023 0 01-1.364.107 5.252 5.252 0 01-2.527-.573 3.883 3.883 0 01-1.638-1.665 5.548 5.548 0 01-.569-2.6 5.5 5.5 0 01.569-2.575 3.964 3.964 0 011.611-1.671 4.965 4.965 0 012.455-.59 4.62 4.62 0 013.089 1.016 1.058 1.058 0 01.234.294.854.854 0 01-.087.843.479.479 0 01-.388.2.737.737 0 01-.267-.047 1.5 1.5 0 01-.281-.153 4.232 4.232 0 00-1.1-.582 3.648 3.648 0 00-1.146-.167 2.747 2.747 0 00-2.2.859 3.834 3.834 0 00-.742 2.561q0 3.477 3.049 3.477a6.752 6.752 0 001.815-.254v-2.36h-1.517a.737.737 0 01-.5-.161.664.664 0 010-.909.732.732 0 01.5-.161zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z" class="a"></path></svg>';
            break;
          case 'sgpoolconfigs':
          case 'sgpoolingconfigs':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 26.5 20"><path d="M14.305 18.749a4.7 4.7 0 01-2.388-.589 3.91 3.91 0 01-1.571-1.685 5.668 5.668 0 01-.546-2.568 5.639 5.639 0 01.548-2.561 3.916 3.916 0 011.571-1.678 4.715 4.715 0 012.388-.593 5.189 5.189 0 011.658.261 4.324 4.324 0 011.378.756.758.758 0 01.24.281.859.859 0 01.067.361.768.768 0 01-.16.495.479.479 0 01-.388.2.984.984 0 01-.548-.191 4 4 0 00-1.07-.595 3.405 3.405 0 00-1.1-.167 2.571 2.571 0 00-2.106.869 3.943 3.943 0 00-.72 2.562 3.963 3.963 0 00.716 2.568 2.568 2.568 0 002.106.869 3.147 3.147 0 001.063-.173 5.112 5.112 0 001.1-.589 2.018 2.018 0 01.267-.134.751.751 0 01.29-.048.477.477 0 01.388.2.767.767 0 01.16.494.863.863 0 01-.067.355.739.739 0 01-.24.286 4.308 4.308 0 01-1.378.757 5.161 5.161 0 01-1.658.257zm5.71-.04a.841.841 0 01-.622-.234.856.856 0 01-.234-.636v-7.824a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.29a3.4 3.4 0 012.354.755 2.7 2.7 0 01.842 2.12 2.725 2.725 0 01-.842 2.127 3.386 3.386 0 01-2.354.764h-2.393v2.875a.8.8 0 01-.87.868zm3.05-5.069q1.779 0 1.779-1.552t-1.779-1.551h-2.18v3.1zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>';
            break;
          case 'sgbackupconfigs':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 27.1 20"><path d="M15.889 13.75a2.277 2.277 0 011.263.829 2.394 2.394 0 01.448 1.47 2.27 2.27 0 01-.86 1.885 3.721 3.721 0 01-2.375.685h-3.449a.837.837 0 01-.6-.213.8.8 0 01-.22-.6v-7.795a.8.8 0 01.22-.6.835.835 0 01.609-.214h3.306a3.679 3.679 0 012.306.648 2.165 2.165 0 01.836 1.815 2.159 2.159 0 01-.395 1.3 2.254 2.254 0 01-1.089.79zm-4.118-.585h2.179q1.779 0 1.778-1.323a1.143 1.143 0 00-.441-.989 2.267 2.267 0 00-1.337-.321h-2.179zm2.407 4.118a2.219 2.219 0 001.363-.335 1.242 1.242 0 00.428-1.042 1.271 1.271 0 00-.435-1.056 2.155 2.155 0 00-1.356-.348h-2.407v2.781zm8.929 1.457a3.991 3.991 0 01-2.941-1 3.968 3.968 0 01-1-2.927V9.984a.854.854 0 01.227-.622.925.925 0 011.23 0 .854.854 0 01.227.622V14.9a2.623 2.623 0 00.573 1.838 2.18 2.18 0 001.684.622 2.153 2.153 0 001.671-.628 2.624 2.624 0 00.573-1.832V9.984a.85.85 0 01.228-.622.924.924 0 011.229 0 .85.85 0 01.228.622v4.826a3.969 3.969 0 01-1 2.92 3.95 3.95 0 01-2.929 1.01zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>';
            break;
          case 'sginstanceprofiles':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 21.3 20"><path d="M10.962 9.14a.808.808 0 00-.862.86v7.878a.86.86 0 00.235.63.83.83 0 00.624.242.82.82 0 00.872-.872V10a.842.842 0 00-.235-.624.862.862 0 00-.634-.236zm9.407.825a3.419 3.419 0 00-2.362-.758h-3.3a.842.842 0 00-.611.215.8.8 0 00-.221.6v7.851a.859.859 0 00.233.637.842.842 0 00.624.235.806.806 0 00.868-.87v-2.882h2.406a3.393 3.393 0 002.362-.767 2.729 2.729 0 00.846-2.133 2.709 2.709 0 00-.845-2.128zm-2.576 3.7H15.6v-3.116h2.192q1.785 0 1.785 1.557t-1.784 1.557zM.955 4.762h10.5a.953.953 0 100-1.9H.955a.953.953 0 100 1.9zM14.8 7.619a.954.954 0 00.955-.952V4.762h4.3a.953.953 0 100-1.9h-4.3V.952a.955.955 0 00-1.909 0v5.715a.953.953 0 00.954.952zM.955 10.952h4.3v1.9a.955.955 0 001.909 0V7.143a.955.955 0 00-1.909 0v1.9h-4.3a.953.953 0 100 1.9zm6.681 4.286H.955a.953.953 0 100 1.905h6.681a.953.953 0 100-1.905z"></path></svg>';
            break;
          case 'sgbackups':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path d="M10.55.55A9.454 9.454 0 001.125 9.5H.479a.458.458 0 00-.214.053.51.51 0 00-.214.671l1.621 3.382a.49.49 0 00.213.223.471.471 0 00.644-.223l1.62-3.382A.51.51 0 004.2 10a.49.49 0 00-.479-.5H3.1a7.47 7.47 0 117.449 7.974 7.392 7.392 0 01-3.332-.781.988.988 0 00-.883 1.767 9.356 9.356 0 004.215.99 9.45 9.45 0 000-18.9z" class="a"></path><path d="M13.554 10a3 3 0 10-3 3 3 3 0 003-3z" class="a"></path></svg>';
            break;
          case 'sgdistributedlogs':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><path class="a" d="M19,15H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,14.6,19.6,15,19,15z"/><path class="a" d="M1,15L1,15c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,14.6,1.6,15,1,15z"/><path class="a" d="M19,11H5c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,10.6,19.6,11,19,11z"/><path class="a" d="M1,11L1,11c-0.6,0-1-0.4-1-1v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,10.6,1.6,11,1,11z"/><path class="a" d="M19,7H5C4.4,7,4,6.6,4,6v0c0-0.6,0.4-1,1-1h14c0.6,0,1,0.4,1,1v0C20,6.6,19.6,7,19,7z"/><path d="M1,7L1,7C0.4,7,0,6.6,0,6v0c0-0.6,0.4-1,1-1h0c0.6,0,1,0.4,1,1v0C2,6.6,1.6,7,1,7z"/></svg>';
            break;
          case 'sgdbops':
            icon = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><g fill="#36A8FF"><path d="M17.1 20c-.6 0-1-.5-1-1 0-1.6-1.3-2.8-2.8-2.8H6.6c-1.6 0-2.8 1.3-2.8 2.8 0 .6-.5 1-1 1s-1-.5-1-1c0-2.7 2.2-4.8 4.8-4.8h6.7c2.7 0 4.8 2.2 4.8 4.8.1.5-.4 1-1 1zM9.9 9.4c-1.4 0-2.5-1.1-2.5-2.5s1.1-2.5 2.5-2.5 2.5 1.1 2.5 2.5c.1 1.4-1.1 2.5-2.5 2.5zm0-3.3c-.4 0-.8.3-.8.8 0 .4.3.8.8.8.5-.1.8-.4.8-.8 0-.5-.3-.8-.8-.8z"/><path d="M10 13.7h-.2c-1-.1-1.8-.8-1.8-1.8v-.1h-.1l-.1.1c-.8.7-2.1.6-2.8-.2s-.7-1.9 0-2.6l.1-.1H5c-1.1 0-2-.8-2.1-1.9 0-1.2.8-2.1 1.8-2.2H5v-.1c-.7-.8-.7-2 .1-2.8.8-.7 1.9-.7 2.7 0 .1 0 .1 0 .2-.1 0-.6.3-1.1.7-1.4.8-.7 2.1-.6 2.8.2.2.3.4.7.4 1.1v.1h.1c.8-.7 2.1-.6 2.8.2.6.7.6 1.9 0 2.6l-.1.1v.1h.1c.5 0 1 .1 1.4.5.8.7.9 2 .2 2.8-.3.4-.8.6-1.4.7h-.3c.4.4.6 1 .6 1.5-.1 1.1-1 1.9-2.1 1.9-.4 0-.9-.2-1.2-.5l-.1-.1v.1c0 1.1-.9 1.9-1.9 1.9zM7.9 10c1 0 1.8.8 1.8 1.7 0 .1.1.2.2.2s.2-.1.2-.2c0-1 .8-1.8 1.8-1.8.5 0 .9.2 1.3.5.1.1.2.1.3 0s.1-.2 0-.3c-.7-.7-.7-1.8 0-2.5.3-.3.8-.5 1.3-.5h.1c.1 0 .2 0 .2-.1 0 0 .1-.1.1-.2s0-.1-.1-.2c0 0-.1-.1-.2-.1h-.2c-.7 0-1.4-.4-1.6-1.1 0-.1 0-.1-.1-.2-.2-.6-.1-1.3.4-1.8.1-.1.1-.2 0-.3s-.2-.1-.3 0c-.3.3-.8.5-1.2.5-1 0-1.8-.8-1.8-1.8 0-.1-.1-.2-.2-.2s-.1 0-.2.1c.1.1 0 .2 0 .3 0 .7-.4 1.4-1.1 1.7-.1 0-.1 0-.2.1-.6.2-1.3 0-1.8-.4-.1-.1-.2-.1-.3 0-.1.1-.1.2 0 .3.3.3.5.7.5 1.2.1 1-.7 1.9-1.7 1.9h-.2c-.1 0-.1 0-.2.1 0-.1 0 0 0 0 0 .1.1.2.2.2h.2c1 0 1.8.8 1.8 1.8 0 .5-.2.9-.5 1.2-.1.1-.1.2 0 .3s.2.1.3 0c.3-.2.7-.4 1.1-.4h.1z"/></g></svg>';
            break;
        }
      
        if(kind === 'error') {
          details = `
            <div class="message show">
              <span class="remove">
                <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
              </span>
              <span class="icon `+kind+`">
                `+icon+`
              </span>
              <span class="kind `+kind+`">
                `+kind+`
              </span>`;
      
          if( (message.status !== 500) && (message.status !== 401) ) {
            
            if(message.hasOwnProperty('title')) {
              details += '<h4 class="title">' + message.title + '</h4>';
            }
            
            details += '<div class="detail">' + message.detail +'</div>';
            
            if(message.hasOwnProperty('type') && message.type.length)
              details += `<a href="`+message.type+`" title="More Info" target="_blank" class="doclink">More Info <svg xmlns="http://www.w3.org/2000/svg" width="15.001" height="12.751" viewBox="0 0 15.001 12.751"><g transform="translate(167.001 -31.5) rotate(90)"><path d="M37.875,168.688a.752.752,0,0,1-.53-.219l-5.625-5.626a.75.75,0,0,1,0-1.061l2.813-2.813a.75.75,0,0,1,1.06,1.061l-2.283,2.282,4.566,4.566,4.566-4.566-2.283-2.282a.75.75,0,0,1,1.06-1.061l2.813,2.813a.75.75,0,0,1,0,1.061l-5.625,5.626A.752.752,0,0,1,37.875,168.688Z" transform="translate(0 -1.687)" fill="#00adb5"/><path d="M42.156,155.033l-2.813-2.813a.752.752,0,0,0-1.061,0l-2.813,2.813a.75.75,0,1,0,1.06,1.061l1.533-1.534v5.3a.75.75,0,1,0,1.5,0v-5.3l1.533,1.534a.75.75,0,1,0,1.06-1.061Z" transform="translate(-0.937 0)" fill="#00adb5"/></g></svg></a>`;
          }
      
          details += '</div>';
          
          if(message.hasOwnProperty('fields')) {
            message.fields.forEach( function(item, index) {
              $("[data-field='"+item+"']").addClass("notValid");
            });
          }
        } else {
          details = `
          <div class="message show">
              <span class="remove">
                <svg xmlns="http://www.w3.org/2000/svg" width="13.5" height="15" viewBox="0 0 13.5 15"><g transform="translate(-61 -90)"><path d="M73.765,92.7H71.513a.371.371,0,0,1-.355-.362v-.247A2.086,2.086,0,0,0,69.086,90H66.413a2.086,2.086,0,0,0-2.072,2.094V92.4a.367.367,0,0,1-.343.3H61.735a.743.743,0,0,0,0,1.486h.229a.375.375,0,0,1,.374.367v8.35A2.085,2.085,0,0,0,64.408,105h6.684a2.086,2.086,0,0,0,2.072-2.095V94.529a.372.372,0,0,1,.368-.34h.233a.743.743,0,0,0,0-1.486Zm-7.954-.608a.609.609,0,0,1,.608-.607h2.667a.6.6,0,0,1,.6.6v.243a.373.373,0,0,1-.357.371H66.168a.373.373,0,0,1-.357-.371Zm5.882,10.811a.61.61,0,0,1-.608.608h-6.67a.608.608,0,0,1-.608-.608V94.564a.375.375,0,0,1,.375-.375h7.136a.375.375,0,0,1,.375.375Z" transform="translate(0)"/><path d="M68.016,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,68.016,98.108Z" transform="translate(-1.693 -3.214)"/><path d="M71.984,98.108a.985.985,0,0,0-.98.99V104.5a.98.98,0,1,0,1.96,0V99.1A.985.985,0,0,0,71.984,98.108Z" transform="translate(-2.807 -3.214)"/></g></svg>
              </span>
              <span class="icon message">
                `+icon+`
              </span>
              <span class="kind message">
                Message
              </span>
              <h4 class="title">`+message+`</h4>
            </div>
          `;
        }
      
        $("#notifications .tooltip").append(details);
        $("#notifications .count").text(parseInt($("#notifications .count").text(),10)+1);
      
        if(parseInt($("#notifications .count").text(),10) > 0) {
          $("#notifications").addClass("active");
          $("#notifications .count").removeClass("zero");
          $("#notifications .tooltip .zero").hide();
        } else {
          $("#notifications .tooltip .zero").show();
        }
      },
  
      showTooltip: function( kind, field, customTooltip = '' ) {
  
        const label = $("[for='"+field+"']").first().text();
        $("#help .title").html(label);

        // If it's a custom tooltip
        if(customTooltip.length) {
          
          store.commit("setTooltipsText", customTooltip);

        } else { // Get tooltips from CRDs
  
          const crd = store.state.tooltips[kind];
          let param = crd;
    
          if(field == 'spec.postgresql.conf') {
            param =  crd.spec['postgresql.conf']
          } else if (field == 'spec.pgBouncer.pgbouncer.ini') {
            param =  crd.spec.pgBouncer['pgbouncer.ini']
          } else {
            let params = field.split('.');
            params.forEach(function(item, index){
              param = param[item]
            })
          }
    
          store.commit("setTooltipsText", param.description);
        }
  
      },
  
      cloneCRD: function(kind, namespace, name) {
  
        var crd = {};
  
        switch(kind) {
          case 'SGClusters':
            crd = JSON.parse(JSON.stringify(store.state.clusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            
            if(this.hasProp(crd, 'data.spec.initialData.restore')) {
              delete crd.data.spec.initialData.restore

              if(!Object.keys(crd.data.spec.initialData).length)
                delete crd.data.spec.initialData
            }

            break;
          
          case 'SGBackupConfigs':
            crd = JSON.parse(JSON.stringify(store.state.backupConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
  
          case 'SGBackups':
            crd = JSON.parse(JSON.stringify(store.state.backups.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGInstanceProfiles':
            crd = JSON.parse(JSON.stringify(store.state.profiles.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGPoolingConfigs':
            crd = JSON.parse(JSON.stringify(store.state.poolConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGPostgresConfigs':
            crd = JSON.parse(JSON.stringify(store.state.pgConfig.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
          
          case 'SGDistributedLogs':
            crd = JSON.parse(JSON.stringify(store.state.logsClusters.find(c => ( (namespace == c.data.metadata.namespace) && (name == c.name) ))))
            break;
        }
        
        if(typeof crd !== 'undefined') {
          crd.kind = kind;
          crd.data.metadata.name = crd.name = 'copy-of-'+crd.data.metadata.name;
          store.commit('setCloneCRD', crd);

          $('#cloneName').val(crd.data.metadata.name);
          $('#cloneNamespace').val(crd.data.metadata.namespace);
          
          $("#notifications.hasTooltip.active").removeClass("active");
          $("#notifications.hasTooltip .message.show").removeClass("show");
        }
      },

      cleanupTooltips( obj ) {
        const vc = this;

        Object.keys(obj).forEach(function(key){
          if(obj[key].hasOwnProperty('type')) {
            if((obj[key].type != 'array') && obj[key].hasOwnProperty('properties')){
              var desc = ''
              
              if(obj[key].hasOwnProperty('description'))
                desc = obj[key].description
        
              obj[key] = obj[key].properties
              obj[key].description = desc
              vc.cleanupTooltips(obj[key])
              
            } else if ( (obj[key].type == 'array') && obj[key].items.hasOwnProperty('properties')){
              Object.keys(obj[key].items.properties).forEach(function(k){
                obj[key][k] = obj[key].items.properties[k]
              })      
            } 
          }
        })
      },

      hasParams( obj, params) {
        var valid = true;
      
        for ( var i=0; i < params.length; i++){
          if( !obj || !obj.hasOwnProperty(params[i]) ) {
            return valid = false
          }
          else
            obj = obj[params[i]]
        }
      
        return valid
      },      

      // Sort tables by an specific parameter
      sortTable( table, param, direction, type = 'alphabetical' ) {
        const vc = this;

        var backupFixedParams = ['data.spec.subjectToRetentionPolicy','data.metadata.name','data.spec.sgCluster','data.status.process.status'];
        
        table.sort((a,b) => {
          let modifier = 1;
          
          if(direction === 'desc') modifier = -1;

          // If sorting backups first validate its state
          if(a.data.hasOwnProperty('status') && a.hasOwnProperty('duration')) {
            // If record is not sortable by the provided param
            if(a.data.status !== null) {
              if( (a.data.status.process.status == 'Failed') && !backupFixedParams.includes(param)){
                return 1
              } else if ((a.data.status.process.status == 'Running') && !backupFixedParams.includes(param)){
                return -1
              }
            }
          } 

          if (param == 'data.status.conditions') { // If dbOps sorted by Status

            if(a.data.hasOwnProperty('status')) {
              a = a.data.status.conditions.find(c => (c.status === 'True') )
              a = a.type
            } else {
              a = '-'
            }

            if(b.data.hasOwnProperty('status')) {
              b = b.data.status.conditions.find(c => (c.status === 'True') )
              b = b.type
            } else {
              b = '-'
            }

          } else if (param == 'data.spec.runAt' ) { // If dbOps sorted by runAt
            
            a = a.data.spec.hasOwnProperty('runAt') ? a.data.spec.runAt : (vc.hasProp(a, 'data.status.opStarted') ? a.data.status.opStarted : '-');
            b = b.data.spec.hasOwnProperty('runAt') ? b.data.spec.runAt : (vc.hasProp(b, 'data.status.opStarted') ? b.data.status.opStarted : '-');

          } else if (param == 'data.status.elapsed' ) { // If dbOps sorted by elapsed
            
            if( a.data.hasOwnProperty('status') ) {
              let lastStatus = a.data.status.conditions.find(c => (c.status === 'True') )
              let begin = moment(a.data.status.opStarted)
              let finish = (lastStatus.type == 'Running') ? moment() : moment(lastStatus.lastTransitionTime);
              a = moment.duration(finish.diff(begin));
            } else {
              a = -1
            }

            if( b.data.hasOwnProperty('status') ) {
              let lastStatus = b.data.status.conditions.find(c => (c.status === 'True') )
              let begin = moment(b.data.status.opStarted)
              let finish = (lastStatus.type == 'Running') ? moment() : moment(lastStatus.lastTransitionTime);
              b = moment.duration(finish.diff(begin));
            } else {
              b = -1
            }

          } else { // Every other param

            if(vc.hasParams( a, param.split(".")))
              a = eval("a."+param);
            else
              a = '';

            if(vc.hasParams( b, param.split(".")))
              b = eval("b."+param);
            else
              b = '';

          }

          switch(type) {

            case 'timestamp':

              if(moment(a).isValid && moment(b).isValid) {

                if(moment(a).isBefore(moment(b)))
                  return -1 * modifier;
              
                if(moment(a).isAfter(moment(b)))
                  return 1 * modifier;  

              } else if (!moment(a).isValid && moment(b).isValid) {
                return -1 * modifier;
              } else if (moment(a).isValid && !moment(b).isValid) {
                return 1 * modifier;
              }
              
              break;

            case 'memory':
              
              if( vc.getBytes(a) < vc.getBytes(b) )
                return -1 * modifier;
              
              if( vc.getBytes(a) > vc.getBytes(b) )
                return 1 * modifier;
              
              break;
            
            default: // alphabetical, duration, cpu

              if(type == 'cpu') {
                a = a.includes('m') ? (parseFloat(a.replace('m','')/1000)) : a;
                b = b.includes('m') ? (parseFloat(b.replace('m','')/1000)) : b;
              }
                          
              if( a < b )
                return -1 * modifier;
              
              if( a > b )
                return 1 * modifier;
              
                break;
            
          }
          
          return 0;
        });

        return table;

      },

      getBytes (text) {
        var powers = {'Ki': 1, 'Mi': 2, 'Gi': 3, 'Ti': 4, 'Pi': 5};
        var regex = /(\d+(?:\.\d+)?)\s?(Ki|Mi|Gi|Ti|Pi)?b?/i;
        var res = regex.exec(text);
        
        return ( (res !== null) && (typeof res[2] !== 'undefined') ) ? (res[1] * Math.pow(1024, powers[res[2]]) ) : text;
      },
      
      hasProp(obj, propertyPath){
        if(!propertyPath)
            return false;
      
        var properties = propertyPath.split('.');
      
        for (var i = 0; i < properties.length; i++) {
            var prop = properties[i];
      
            if(!obj || !obj.hasOwnProperty(prop)){
                return false;
            } else {
                obj = obj[prop];
            }
        }
      
        return true;
      },

      goTo(path) {
        var $target = $(event.target);

        if( !$target.closest('td.actions').length) {
        
          if(window.location.href.endsWith(path)) // Already in resource, go to parent page
            router.push(path.substring(0, path.lastIndexOf("/") + 1))
          else
            router.push(path)

        }
      },

      getTooltip(field) {
        const vc = this;

        if(Object.keys(vc.tooltips).length) {
          if(field == 'sgpostgresconfig.spec.postgresql.conf') {
            return vc.tooltips.sgpostgresconfig.spec['postgresql.conf'].description
          } else if (field == 'sgpoolingconfig.spec.pgBouncer.pgbouncer.ini') {
            return vc.tooltips.sgpoolingconfig.spec.pgBouncer['pgbouncer.ini'].description
          } else if (vc.hasProp(vc.tooltips, field)) {
            let params = field.split('.');
            let tooltipText = vc.tooltips;

            params.forEach(function(item, index){
              tooltipText = tooltipText[item]
            })
            return tooltipText.description
          } else {
            return 'Information not available'
          }
        } else {
          return 'Information not available'
        }
        
      },

      tzCrontab( baseCrontab, toLocal = true ) {

        if( !!moment().utcOffset() && (store.state.timezone == 'local') ) {    

          let crontab = baseCrontab.split(' ');
          
          const isParsable = function(n) {
            try {
              var t = parseInt(n);
              return Number.isInteger(t)
            } catch(err) {
                return false
            }
          }

          if(isParsable(crontab[1])) {
            
            let tzOffset = (moment().utcOffset() / 60);      
            let dom = crontab[2];
            let dow = crontab[4];
            let modifier = 0;

            //console.log(tzOffset)
            
            crontab[1] = parseInt( crontab[1] ) + ( tzOffset * ( toLocal ? 1 : -1 ) ); // Set opposite offset if converting to UTC

            if(!crontab[1].isInteger) {
              
              // Fix minutes offset if in timezone with 30/45min offsets
              if (isParsable(crontab[0])) {
                let minOffset = crontab[1] % 1;

                if(crontab[0].includes('-')) {
                  crontab[0] = (parseInt(crontab[0].split('-')[0]) + (minOffset * 60)) + '-' + (parseInt(crontab[0].split('-')[1]) + (minOffset * 60) );
                } else if(crontab[0].includes('/')) {
                  crontab[0] = (parseInt(crontab[0].split('/')[0]) + (minOffset * 60)) + '/' + (parseInt(crontab[0].split('/')[1]) + (minOffset * 60) );
                } else  {
                  crontab[0] = (parseInt(crontab[0] + (minOffset * 60)) );
                }
              }

              crontab[1] = parseInt( crontab[1] )
            }

            // Fix hour offset on 24h edges
            if(crontab[1] < 0) {
              modifier = -1
              crontab[1] = crontab[1] + 24
            } else if(crontab[1] >= 24) {
              modifier = 1
              crontab[1] = crontab[1] - 24
            }
            
            if(dom.includes('-')) {
              crontab[2] = (parseInt(dom.split('-')[0]) + modifier) + '-' + (parseInt(dom.split('-')[1]) + modifier )
            } else if (dom.includes('/')) {
              crontab[2] = (parseInt(dom.split('/')[0]) + modifier) + '/' + (parseInt(dom.split('/')[1]) + modifier )
            } else if (isParsable(dom)) {
              crontab[2] = (parseInt(dom) + modifier)
      
              // Fix DOM offset on month edges
              if(crontab[2] < 1) {

                var month = crontab[3];
                // Offset month 
                if(isParsable(month) && parseInt(month) > 1) { 
                  crontab[3] = (parseInt(crontab[3]) - 1)
                } else { // Jan > Dec
                  crontab[3] = 12
                }
      
                // Offset day of month
                if( [1,3,5,7,8,10,12].includes(crontab[3]) ) { // Jan, Mar, May, Jul, Aug, Oct, Dec
                  crontab[2] = 31
                } else if ([4,6,9,11].includes(crontab[3])) { // Apr, Jun, Sept, Nov
                  crontab[2] = 30
                } else if (crontab[3] == 2) { // Feb
                  crontab[2] = 28
                }
      
              } else if (
                (crontab[2] > 31) || 
                ((crontab[2] > 30) && (['4','6','9','11'].includes(crontab[3]))) ||
                ((crontab[2] > 28) && (crontab[3] == '2')) ) {
                
                  // Offset month 
                  if(parseInt(crontab[3]) < 12) { 
                    crontab[3] = (parseInt(crontab[3]) + 1);
                  } else { // Dec > Jan
                    crontab[3] = 1
                  }
                  
                  crontab[2] = 1;
      
              }
      
            }
      
            if(!isParsable(dow) && dow.includes('-')) {
              crontab[4] = (parseInt(dow.split('-')[0]) + modifier) + '-' + (parseInt(dow.split('-')[1]) + modifier )
            } else if (dow.includes('/')) {
              crontab[4] = (parseInt(dow.split('/')[0]) + modifier) + '/' + (parseInt(dow.split('/')[1]) + modifier )
            } else if (dow !== '*') {
              crontab[4] = (parseInt(dow) + modifier)
            }
              
            baseCrontab = crontab.join(' ')
          }
      
        } 

        return baseCrontab
      },

      showTzOffset() {
        if( !!moment().utcOffset() && (store.state.timezone == 'local') ) {
          var offset = new Date().getTimezoneOffset(), o = Math.abs(offset);
          return (offset < 0 ? "+" : "-") + ("00" + Math.floor(o / 60)).slice(-2) + ":" + ("00" + (o % 60)).slice(-2);
        } else {
          return '+00:00'
        }
      },

      getIsoDuration(duration) {
        let d = (duration.split('P').pop().split('D')[0] != '0') ? duration.split('P').pop().split('D')[0] : ''
        let h = (duration.split('T').pop().split('H')[0] != '0') ? duration.split('T').pop().split('H')[0] : ''
        let m = (duration.split('H').pop().split('M')[0] != '0') ? duration.split('H').pop().split('M')[0] : ''
        let s = (duration.split('M').pop().split('S')[0] != '0') ? duration.split('M').pop().split('S')[0] : ''

        return (
            (d.length ? (d + ' day' + ( (d != '1') ? 's' : '' ) ) : '') +
            (h.length ? ( (d.length ? ', ' : '') + (h + ' hour' + ( (h != '1') ? 's' : '' ) ) ) : '') +
            (m.length ? ( ( (d.length || h.length) ? ', ' : '') + (m + ' minute' + ( (m != '1') ? 's' : '' ) ) ) : '') +
            (s.length ? ( ( (d.length || h.length || m.length) ? ', ' : '') + (s + ' second' + ( (s != '1') ? 's' : '' ) ) ) : '')
        )
      },

      lookupCRDs(kind, crds) {
        const vc = this;

        switch(kind) {
          
          case 'sgcluster':
            kind = 'clusters'
            break;
          
          case 'sgbackup':
            kind = 'backups'
            break;

          case 'sgpgconfig':
            kind = 'pgConfig'
            break;

          case 'sgpoolconfig':
            kind = 'poolConfig'
            break;

          case 'sgbackupconfig':
            kind = 'backupConfig'
            break;
          
          case 'sginstanceprofile':
            kind = 'profiles'
            break;
          
          case 'sgdistributedlogs':
            kind = 'logsClusters'
            break;
          
          case 'sgdbops':
            kind = 'dbOps'
            break;
        }

        store.state[kind].forEach(function(item, index) {

          let foundItem = crds.find(e => (e.metadata.name == item.data.metadata.name) && (e.metadata.namespace == item.data.metadata.namespace))

          if(typeof foundItem == 'undefined') {

            store.commit('removeResource', {
              kind: kind,
              name: item.data.metadata.name,
              namespace: item.data.metadata.namespace,
            })

            if(vc.$route.params.hasOwnProperty('name') && (item.data.metadata.name == vc.$route.params.name)) {

              switch(kind) {
          
                case 'clusters':
                  router.push('/' + item.data.metadata.namespace + '/sgclusters')
                  break;
                
                case 'pgConfig':
                  router.push('/' + item.data.metadata.namespace + '/sgpgconfigs')
                  break;
      
                case 'poolConfig':
                  router.push('/' + item.data.metadata.namespace + '/sgpoolconfigs')
                  break;
      
                case 'backupConfig':
                  router.push('/' + item.data.metadata.namespace + '/sgbackupconfigs')
                  break;
                
                case 'profiles':
                  router.push('/' + item.data.metadata.namespace + '/sginstanceprofiles')
                  break;
                
                case 'logsClusters':
                  router.push('/' + item.data.metadata.namespace + '/sgdistributedlogs')
                  break;
                
                case 'dbOps':
                  router.push('/' + item.data.metadata.namespace + '/sgdbops')
                  break;
              }

              vc.notify('The resource you were browsing has been deleted from the server')
            } else if ((kind == 'backups') && vc.$route.params.hasOwnProperty('backupname') && (item.data.metadata.name == vc.$route.params.backupname) ) {
              
              if(vc.$route.params.hasOwnProperty('name')) {
                router.push('/' + item.data.metadata.namespace + '/sgcluster/' + vc.$route.params.name + '/sgbackups')
              } else {
                router.push('/' + item.data.metadata.namespace + '/sgbackups')
              }
              vc.notify('The resource you were browsing has been deleted from the server')

            }  

          }

        })
      },

      // Pagination handle
      pageChange(pInfo){
        const vc = this;
        
        vc.pagination.start = pInfo.pageSize * (pInfo.pageNumber-1);
        vc.pagination.end = vc.pagination.start + pInfo.pageSize;
        
      },

      setRestartCluster(namespace = '', name = '') {
        store.commit('setRestartCluster', {namespace: namespace, name: name})
      },

      checkRequired() {
        const vc = this;
        let isValid = true;
        
        $('[required]').each(function() {
          if ( !$(this).val() || ($(this).is(':checkbox') && !$(this).is(':checked')) ) {
            $(this).addClass("notValid");

            if($(this).is(':checkbox')) {
              $(this).parent('label').addClass('notValid');
            }
            isValid = false;
          } else if ($(this).hasClass('error')) {
            $(this).removeClass('notValid');
          }
        });

        if(!isValid && ($('#notifications .message.show.title').text != 'Please fill every mandatory field in the form') ) {
          setTimeout(function() {
            vc.notify('Please fill every mandatory field in the form', 'message', 'general');
          }, 100);
        }

        return isValid
      },

      splitUppercase(text) {
        return text.split(/(?=[A-Z])/).join(' ')
      },

      getDateString() {
        let fullDate = new Date();
        let timeString = (store.state.timezone == 'local' ? fullDate.toLocaleTimeString().replaceAll(':','-') : fullDate.toUTCString().slice(fullDate.toUTCString().lastIndexOf(' ') - 8, fullDate.toUTCString().lastIndexOf(' ')).replaceAll(':','-'));
        let localeMonth = fullDate.getMonth()+1;
        let dayString = (store.state.timezone == 'local' ? (fullDate.getFullYear() + '-' + localeMonth.toString().padStart(2, '0') + '-' + fullDate.getDate().toString().padStart(2, '0'))  : fullDate.toISOString().slice(0, fullDate.toISOString().indexOf('T')));

        let dateString = dayString + '-' + timeString

        return dateString
      }

    },
  
    beforeCreate: function() {
      store.commit('setTooltipsText','Click on a question mark to get help and tips about that field.');
    },

    mounted: function() {
      const vc = this;

      $(window).on('resize', function() {
        vc.pagination.rows = parseInt(($(window).innerHeight() - 480)/40)
        vc.pagination.rows = (vc.pagination.rows <= 0) ? 1 : vc.pagination.rows
        vc.pageChange({
          pageNumber: 1,
          pageSize: vc.pagination.rows
        })
      });
      $(window).resize()

      // Setup currentPath for sidebar use
      store.commit('setCurrentPath', {
        namespace: vc.$route.params.hasOwnProperty('namespace') ? vc.$route.params.namespace : '',
        name: vc.$route.params.hasOwnProperty('name') ? vc.$route.params.name : '',
        component: vc.$route.name.length ? vc.$route.name : ''
      });

      // Allow API fetching from child browser tabs
      window.fetchParentAPI = function(kind) {
        vc.fetchAPI(kind);
      }

    },

    updated: function(){
      const vc = this

      // Little hack to set container height
      vc.$nextTick(function () {
        if( (typeof vc.$refs.page != 'undefined') && $('.v-pagination').length && !$('.content').hasClass('withPagination'))
          $('.content').addClass('withPagination')
        else if (!$('.v-pagination').length)
          $('.content').removeClass('withPagination')
      })
    }
}