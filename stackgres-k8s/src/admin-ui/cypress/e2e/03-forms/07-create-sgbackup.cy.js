describe('Create SGBackup', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;
    let clusterStorageName;
    let clusterName;
    let storageName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6);
        clusterStorageName = 'cluster-storage-' + resourceName;
        clusterName = 'cluster-' + resourceName;
        storageName = 'storage-' + resourceName

        cy.createCRD('sgobjectstorages', {
            metadata: {
                namespace: namespace,
                name: storageName,
            },
            spec: {
                type: 's3Compatible',
                s3Compatible:{
                    forcePathStyle: true,
                    bucket: 'bucket',
                    awsCredentials: {
                        accessKeyId: 'api-key',
                        secretAccessKey: 'api-secret'
                    },
                    region: 'region',
                    endpoint: 'https://endpoint'
                }
            }
        });

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
                postgres: {
                    version: "13.3",
                    flavor: "vanilla"
                }
            }  
        });

        cy.createCRD('sgclusters', {
            metadata: {
                name: clusterStorageName,
                namespace: namespace
            },
            spec: {
                configurations: {
                    backups: [
                        {
                            compression: 'lz4',
                            cronSchedule: '0 3 * * *',
                            path: '',
                            performance: {
                                maxNetworkBandwidth: '', 
                                maxDiskBandwidth: '', 
                                uploadDiskConcurrency: 1
                            },
                            retention: 5,
                            sgObjectStorage: storageName
                        }
                    ]
                },
                instances: 1, 
                pods: {
                    persistentVolume: {
                        size: "128Mi"
                    }
                },
                postgres: {
                    version: "13.3",
                    flavor: "vanilla"
                }
            }  
        });
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit(namespace + '/sgbackups/new')
    });

    after( () => {
        cy.login()

        cy.deleteCluster(namespace, clusterName);

        cy.deleteCluster(namespace, clusterStorageName);

        cy.deleteCRD('sgobjectstorages', {
            metadata: {
                name: storageName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgbackups', {
            metadata: {
                name: 'backup-' + resourceName,
                namespace: namespace
            }
        });

        cy.deleteCRD('sgbackups', {
            metadata: {
                name: 'backup-error-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGBackup form should be visible', () => {
        cy.get('form#createBackup')
            .should('be.visible')
    });

    it('Creating a SGBackup should be possible', () => {
        // Test SGBackup Name
        cy.get('[data-field="metadata.name"]')
            .clear()
            .type('backup-' + resourceName)

        // Test source SGCluster
        cy.get('select[data-field="spec.sgCluster"]', { timeout:10000 })
            .select(clusterStorageName)

        cy.get('label[data-field="spec.managedLifecycle"]')
            .click()

        // Test Submit form
        cy.get('form#createBackup button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Backup "backup-' + resourceName + '" started successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgbackups')
    });

    it('Creating a SGBackup with a default name should be possible', () => {
        // Test source SGCluster
        cy.get('select[data-field="spec.sgCluster"]', { timeout:10000 })
            .select(clusterStorageName)

        // Test Submit form
        cy.get('form#createBackup button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('started successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgbackups')

    });

    //it('Creating a SGBackup for a SGCluster without SGObjectStorage Configuration shoud not be possible', () => {
    //    cy.get('[data-field="metadata.name"]')
    //        .clear()
    //        .type('backup-error-' + resourceName)
    //
    //    cy.get('select[data-field="spec.sgCluster"]', { timeout:10000 })
    //        .select(clusterName)
    //
    //    cy.get('form#createBackup button[type="submit"]')
    //        .click()
    //    
    //    cy.get('#notifications .message.show.error')
    //        .should('be.visible')
    //});
  })