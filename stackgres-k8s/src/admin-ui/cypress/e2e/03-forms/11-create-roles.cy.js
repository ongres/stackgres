describe('Create Roles', () => {
    Cypress.on('uncaught:exception', (err, runnable) => {
      return false
    });

    let resourceName;
    const namespace = Cypress.env('k8s_namespace')
    
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

    it('Creating a basic role should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#roles')
            .should('be.visible')

        cy.get('a[data-field="CreateRole"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/roles/new')
        
        cy.get('form#CreateRole')
            .should('be.visible')

        cy.get('input[data-field="metadata.name"]')
            .clear()
            .type('basic-role-' + resourceName)

        cy.get('select[data-field="metadata.namespace"]')
            .select(namespace)

        // Test Submit form
        cy.get('form#CreateRole button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('Role "basic-role-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/users')
    });

    it('Creating a full role should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#roles')
            .should('be.visible')

        cy.get('a[data-field="CreateRole"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/roles/new')
        
        cy.get('form#CreateRole')
            .should('be.visible')

        cy.get('input[data-field="metadata.name"]')
            .clear()
            .type('full-role-' + resourceName)

        cy.get('select[data-field="metadata.namespace"]')
            .select(namespace)

        
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
                expect($notification).contain('Role "full-role-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/users')
    });

    it('Editing a role should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#roles')
            .should('be.visible')

        cy.get('a.editCRD[data-crd-name="full-role-' + resourceName + '"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/role/full-role-' + resourceName + '/edit')
        
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
                expect($notification).contain('Role "full-role-' + resourceName + '" updated successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/role/full-role-' + resourceName)
    });

  })