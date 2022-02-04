describe('Create SGPostgresConfig', () => {
    
    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()

        generateRandomString = () => Cypress._.random(0, 1e6)

        resourceName = generateRandomString()
    });

    beforeEach( () => {
        Cypress.Cookies.preserveOnce('sgToken')
        cy.visit(namespace + '/sgpgconfigs/new')
    });

    after( () => {
        cy.deleteCRD('sgpgconfigs', {
            metadata: {
                name: 'pgconfig-' + resourceName,
                namespace: namespace
            }
        });
    });

    it('Create SGPostgresConfig form should be visible', () => {
        cy.get('form#cretaePgConfig')
            .should('be.visible')
    });  

    it('Creating a SGPostgresConfig should be possible', () => {
        // Test Config Name
        cy.get('[data-field="metadata.name"]')
            .type('pgconfig-' + resourceName)

        // Test PG Version
        cy.get('[data-field="spec.postgresVersion"]')
            .select('14')
        
        // Test Parameter textarea
        cy.get('[data-field="spec.postgresql.conf"]')
            .type('autovacuum_max_workers = 2')

        // Test Submit form
        cy.get('form#cretaePgConfig button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Postgres configuration "pgconfig-' + resourceName + '" created successfully')
            })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgpgconfigs')

    });

})