describe('Create Cluster Roles', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    let resourceName;
    
    before( () => {
        cy.login()
        resourceName = Cypress._.random(0, 1e6);
    });

    beforeEach( () => {
      cy.gc()
      cy.login()
      cy.setCookie('sgReload', '0')
      cy.setCookie('sgTimezone', 'utc')
      cy.visit('/')
    });

    it('User management link should be visible', () => {
        cy.get('#usersManagement > a')
            .should('be.visible')
    });

    it('Creating a basic cluster role should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#clusterroles')
            .should('be.visible')

        cy.get('a[data-field="CreateClusterRole"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/clusterroles/new')
        
        cy.get('form#CreateRole')
            .should('be.visible')

        cy.get('input[data-field="metadata.name"]')
            .clear()
            .type('basic-clusterrole-' + resourceName)

        // Test Submit form
        cy.get('form#CreateRole button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster Role "basic-clusterrole-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/users')
    });

    it('Creating a full cluster role should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#clusterroles')
            .should('be.visible')

        cy.get('a[data-field="CreateClusterRole"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/clusterroles/new')
        
        cy.get('form#CreateRole')
            .should('be.visible')

        cy.get('input[data-field="metadata.name"]')
            .clear()
            .type('full-clusterrole-' + resourceName)

        // Add Ruleset
        cy.get('a[data-field="add-rule"]')
            .click()

        // Add ApiGroups
        cy.get('a[data-field="add-apiGroups"]')
            .click()

        cy.get('input[data-field="rules[0].apiGroups[0]"]')
            .clear()
            .type('apiGroup-0-' + resourceName);

        // Add nonResourceURLs
        /*  cy.get('a[data-field="add-nonResourceURLs"]')
            .click()

        cy.get('input[data-field="rules[0].nonResourceURLs[0]"]')
            .clear()
            .type('nonResourceURLs-0-' + resourceName);*/

        // Add resourceNames
        cy.get('a[data-field="add-resourceNames"]')
            .click()

        cy.get('input[data-field="rules[0].resourceNames[0]"]')
            .clear()
            .type('resourceNames-0-' + resourceName);
        
        // Add resources
        cy.get('a[data-field="add-resources"]')
            .click()

        cy.get('input[data-field="rules[0].resources[0]"]')
            .clear()
            .type('sgcluster');

        // Add verbs
        cy.get('a[data-field="add-verbs"]')
            .click()

        cy.get('input[data-field="rules[0].verbs[0]"]')
            .clear()
            .type('get');

        cy.get('a[data-field="add-verbs"]')
            .click()

        cy.get('input[data-field="rules[0].verbs[1]"]')
            .clear()
            .type('list');
        
        cy.get('a[data-field="add-verbs"]')
            .click()

        cy.get('input[data-field="rules[0].verbs[2]"]')
            .clear()
            .type('create');

        // Test Submit form
        cy.get('form#CreateRole button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Role "full-clusterrole-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/users')
    });

    it('Editing a cluster role should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#clusterroles')
            .should('be.visible')

        cy.get('a.editCRD[data-crd-name="full-clusterrole-' + resourceName + '"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/clusterrole/full-clusterrole-' + resourceName + '/edit')
        
        cy.get('form#CreateRole')
            .should('be.visible')

        // Add ApiGroups
        cy.get('a[data-field="add-apiGroups"]')
            .click()

        cy.get('input[data-field="rules[0].apiGroups[1]"]')
            .clear()
            .type('apiGroup-1-' + resourceName);

        // Add nonResourceURLs
        /*
        cy.get('a[data-field="add-nonResourceURLs"]')
            .click()

        cy.get('input[data-field="rules[0].nonResourceURLs[1]"]')
            .clear()
            .type('nonResourceURLs-1-' + resourceName); */

        // Add resourceNames
        cy.get('a[data-field="add-resourceNames"]')
            .click()

        cy.get('input[data-field="rules[0].resourceNames[1]"]')
            .clear()
            .type('resourceNames-1-' + resourceName);
        
        // Add resources
        cy.get('a[data-field="add-resources"]')
            .click()

        cy.get('input[data-field="rules[0].resources[1]"]')
            .clear()
            .type('sgshardedcluster');

        // Add verbs
        cy.get('a[data-field="add-verbs"]')
            .click()

        cy.get('input[data-field="rules[0].verbs[3]"]')
            .clear()
            .type('patch');

        cy.get('a[data-field="add-verbs"]')
            .click()

        cy.get('input[data-field="rules[0].verbs[4]"]')
            .clear()
            .type('delete');

        // Test Submit form
        cy.get('form#CreateRole button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Cluster Role "full-clusterrole-' + resourceName + '" updated successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/clusterrole/full-clusterrole-' + resourceName)
    });

  })