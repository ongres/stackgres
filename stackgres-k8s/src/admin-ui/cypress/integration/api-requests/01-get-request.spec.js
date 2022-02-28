describe('Load StackGres version', () => {

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;
    let clusterName;
    let dbopName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6);
        clusterName = 'cluster-' + resourceName;
        dbopName = 'dbop-' + resourceName;

        cy.createCRD('sgclusters', {
            metadata: {
                name: clusterName,
                namespace: namespace
            },
            spec: {
                instances: 1, 
                pods: {
                    persistentVolume: {
                        size: "128Mi"
                    }
                },
                nonProductionOptions: {
                    disableClusterPodAntiAffinity: true
                },
                postgres: {
                    version: "13.3",
                    extensions: [{
                        name: "pg_repack",
                        version: "1.4.7",
                        publisher: "com.ongres",
                        repository: "https://extensions.stackgres.io/postgres/repository"
                    }],
                    flavor: "vanilla"
                }
            }  
        });

        cy.createCRD('sgdbops', {
            metadata: {
                name: dbopName,
                namespace: namespace
            },
            spec: {
                maxRetries: 0,
                op: "restart",
                restart: {
                    method: "InPlace",
                    onlyPendingRestart: false
                },
                sgCluster: clusterName
            }
        })

    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
    });

    after( () => {
        cy.deleteCluster(namespace, clusterName);

    
        cy.deleteCRD('sgdbops', {
            metadata: {
                name: dbopName,
                namespace: namespace
            }
        });
    });

    it('GET can-i', () => {
        cy.getResources('auth/rbac/can-i');
    });

    it('GET postgresql versions', () => {
        cy.getResources('version/postgresql');
    });

    it('GET extensions', () => {
        cy.getResources('extensions/latest');
    });

    it('GET namespaces', () => {
        cy.getResources('namespaces');
    });

    it('GET sgclusters', () => {
        cy.getResources('sgclusters');
    });

    it('GET sginstanceprofiles', () => {
        cy.getResources('sginstanceprofiles');
    });

    it('GET sgpgconfigs', () => {
        cy.getResources('sgpgconfigs');
    });

    it('GET sgpoolconfigs', () => {
        cy.getResources('sgpoolconfigs');
    });

    it('GET sgbackupconfigs', () => {
        cy.getResources('sgbackupconfigs');
    });

    it('GET storageclasses', () => {
        cy.getResources('storageclasses');
    });

    it('GET sgdistributedlogs', () => {
        cy.getResources('sgdistributedlogs');
    });

    it('GET sgbackups', () => {
        cy.getResources('sgbackups');
    });

    it('GET sgdbops', () => {
        cy.getResources('sgdbops');
    });

    it('GET dbop events', () => {
        cy.getResources('namespaces/' + namespace + '/sgdbops/' + dbopName + '/events');
    });

    it('GET configmaps', () => {
        cy.getResources('namespaces/' + namespace + '/configmaps');
    });

    it('GET secrets', () => {
        cy.getResources('namespaces/' + namespace + '/secrets');
    });
    
  })