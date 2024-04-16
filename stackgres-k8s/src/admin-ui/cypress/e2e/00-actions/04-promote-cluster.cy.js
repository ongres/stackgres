describe('Create SGCluster', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        resourceName = Cypress._.random(0, 1e6)

        // Create SGCluster dependency for spec.replicateFrom
        cy.createCRD('sgclusters', {
            metadata: {
                name: 'rep-sgcluster-' + resourceName, 
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
                    version: "latest",
                    flavor: "vanilla"
                }
            }  
        });

        // Create SGCluster to promot
        cy.createCRD('sgclusters', {
            metadata: {
                name: 'promote-sgcluster-' + resourceName, 
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
                    version: "latest",
                    flavor: "vanilla"
                },
                replicateFrom: {
                    instance: {
                        sgCluster: 'rep-sgcluster-' + resourceName
                    }
                }
            }  
        });

    });

    beforeEach( () => {
        cy.gc()
        cy.login()
        cy.setCookie('sgReload', '0')
        cy.setCookie('sgTimezone', 'utc')
    });

    after( () => {
        cy.login()
        cy.deleteCluster(namespace, 'rep-sgcluster-' + resourceName);
        cy.deleteCluster(namespace, 'promote-sgcluster-' + resourceName);
    });

    it('Promoting a standby SGCluster should be possible', () => {
        cy.visit(namespace + '/sgcluster/promote-sgcluster-' + resourceName)

        cy.get('.crdActionLinks a[data-field="promote-sgcluster"]')
            .should('be.visible')
            .click()

        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster "promote-sgcluster-' + resourceName + '" promoted successfully')
            })
        
        cy.get('.crdActionLinks a[data-field="promote-sgcluster"]')
            .should('not.exist')
    });
})