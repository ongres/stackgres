describe('Create SGScripts', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });
  
    const namespace = Cypress.env('k8s_namespace')
    let resourceName;

    before( () => {
        cy.login()
        
        resourceName = Cypress._.random(0, 1e6)
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit(namespace + '/sgscripts/new')
    });

    after( () => {
        cy.login()

        cy.deleteCRD('sgscripts', {
            metadata: {
                name: 'script-' + resourceName,
                namespace: namespace
            }
        });
    });

    
    it('Create SGScripts form should be visible', () => {
        cy.get('form#createScripts')
            .should('be.visible')
    });  

    it('Createing a raw script should be possible', () => {
        // Test Script Name
        cy.get('[data-field="metadata.name"]')
            .type('script-' + resourceName)

        // Test Entry Name
        cy.get('[data-field="spec.scripts[0].name"]')
            .type('entry-1')  
            
        // Test Entry Database
        cy.get('[data-field="spec.scripts[0].database"]')
            .type('postgres')  

        // Test Entry User
        cy.get('[data-field="spec.scripts[0].user"]')
            .type('postgres') 
            
        // Test Entry script textarea
        cy.get('[data-field="spec.scripts[0].script"]')
            .type(resourceName)

        // Test Dry Run
        cy.get('form#createScripts button[data-field="dryRun"]')
            .click()

        cy.get('#crdSummary')
            .should('be.visible')

        cy.get('#crdSummary span.close')
            .click()
        
        cy.get('#crdSummary')
            .should('not.exist')
        
        // Test Submit form
        cy.get('form#createScripts button[type="submit"]')
            .click()
    
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Script configuration "script-' + resourceName + '" created successfully')
        })

        cy.location('pathname').should('eq', '/admin/' + namespace + '/sgscripts')
    }); 


    it('Adding a new entry should be possible', () => {
        // Test Add New Entry button
        cy.get('a.addRow')
            .click()
        
        // Test new entry visibility
        cy.get('[data-field="spec.scripts[1].name"]')
            .should('be.visible')        
    }); 

    it('Deleting an entry should be possible', () => {
        // Test Delete Entry button
        cy.get('div.header div.addRow > a:first-of-type')
            .click()
        
        // Test new entry visibility
        cy.get('[data-field="spec.scripts[0].name"]')
            .should('not.exist')
            
    }); 
 
})
