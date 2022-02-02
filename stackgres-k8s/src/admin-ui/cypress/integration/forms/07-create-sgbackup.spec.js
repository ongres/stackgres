describe('Create SGBackup', () => {

    const namespace = Cypress.env('namespace')
    let resourceName;
    let clusterName;

    before( () => {
        cy.login()

        generateRandomString = () => Cypress._.random(0, 1e6)

        resourceName = generateRandomString()
        clusterName = 'cluster-' + resourceName

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
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/sgbackups/new')
    });

    after( () => {
        cy.deleteCluster(namespace, clusterName);

        cy.deleteCRD('sgbackups', {
            metadata: {
                name: 'backup-' + resourceName,
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
            .select(clusterName)

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
  })