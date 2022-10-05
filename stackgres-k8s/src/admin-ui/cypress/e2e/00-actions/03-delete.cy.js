describe('Delete StackGres Resources', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    Cypress.Commands.add('testDelete', (kind, name) => {
        
        // Visit resource page
        cy.visit(namespace + '/' + kind + '/' + name);

        
        // Begin delete process
        cy.get('.crdActionLinks a.deleteCRD')
            .click()
        
        cy.get('#delete > .tooltip')
            .should('be.visible')

        cy.get('#deleteName')
            .type(name)
        
        cy.get('#delete .confirmDelete')
            .click()
        
        // Test notification
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Resource "' + name + '" deleted successfully')
            })
        
        // Test user redirection
        cy.location('pathname').should('eq', '/admin/' + namespace + '/' + kind + 's')
    });
    
    before( () => {
        cy.login()
        resourceName = 'resource-' + Cypress._.random(0, 1e6);
        
        cy.createCRD('sgclusters', {
            metadata: {
                name: resourceName,
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
                    version: "latest"
                }
            }  
        });

        // Disable temporarily until IDs for each backup row has been set
        /* cy.createCRD('sgbackups', {
            metadata: {
                name: resourceName,
                namespace: namespace
            },
            spec: {
                sgCluster: resourceName
            }
        }); */

        cy.createCRD('sginstanceprofiles', {
            metadata: {
                name: resourceName,
                namespace: namespace
            },
            spec: {
                cpu: 1,
                memory: '1Gi'
            }
        }) 

        cy.createCRD('sgpgconfigs', {
            metadata: {
                name: resourceName,
                namespace: namespace
            },
            spec: {
                postgresVersion: 13
            }
        })

        cy.createCRD('sgpoolconfigs', {
            metadata: {
                name: resourceName,
                namespace: namespace
            }
        })

        cy.createCRD('sgscripts', {
            metadata: {
                name: resourceName, 
                namespace: namespace
            },
            spec: {
                continueOnError: false,
                managedVersions: true,
                scripts: [
                    {
                        storeStatusInDatabase: false, 
                        retryOnError: false, 
                        script: resourceName
                    }
                ]
            } 
        })

        cy.createCRD('sgdistributedlogs', {
            metadata: {
                name: resourceName,
                namespace: namespace
            },
            spec: {
                persistentVolume: {
                    size: "128Mi"
                }
            }  
        });

        cy.createCRD('sgdbops', {
            metadata: {
                name: resourceName,
                namespace: namespace
            },
            spec: {
                op: "restart",
                sgCluster: resourceName
            }
        });

    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
  });
    
    it( 'Deleting an SGDbOp should be possible', () => {
        cy.testDelete('sgdbop', resourceName)
    })

    it( 'Deleting an SGCluster should be possible', () => {
        cy.testDelete('sgcluster', resourceName)
    });

    it( 'Deleting an SGInstanceProfile should be possible', () => {
        cy.testDelete('sginstanceprofile', resourceName)
    }); 

    it( 'Deleting an SGPostgresConfig should be possible', () => {
        cy.testDelete('sgpgconfig', resourceName)
    });

    it( 'Deleting an SGPoolingConfig should be possible', () => {
        cy.testDelete('sgpoolconfig', resourceName)
    });

    it( 'Deleting a SGScript should be possible', () => {
        cy.testDelete('sgscript', resourceName)
    });

    it( 'Deleting an SGDistributedLog should be possible', () => {
        cy.testDelete('sgdistributedlog', resourceName)
    })

    // Disable temporarily until IDs for each backup row has been set
    /* it( 'Deleting an SGBackup should be possible', () => {
        cy.testDelete('sgbackup', resourceName)
    }); */

})