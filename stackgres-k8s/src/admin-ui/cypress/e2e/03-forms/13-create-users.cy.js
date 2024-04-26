describe('Create Users', () => {
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

    it('Creating a basic user should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#users')
            .should('be.visible')

        cy.get('a[data-field="CreateUser"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/users/new')
        
        cy.get('form#CreateUser')
            .should('be.visible')

        cy.get('input[data-field="metadata.name"]')
            .clear()
            .type('user-' + resourceName)

        cy.get('input[data-field="k8sUsername"]')
            .clear()
            .type('k8s-username-' + resourceName)

        cy.get('input[data-field="password"]')
            .clear()
            .type(resourceName)

        cy.get('input[data-field="apiUsername"]')
            .clear()
            .type('api-username-' + resourceName)

        // Test Submit form
        cy.get('form#CreateUser button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('User "user-' + resourceName + '" created successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/users')
    });    

    it('Editing a user should be possible', () => {
        cy.get('#usersManagement > a')
            .click()
        
        cy.get('table#users')
            .should('be.visible')

        cy.get('a.editCRD[data-crd-name="user-' + resourceName + '"]')
            .click()

        cy.location('pathname')
            .should('eq', '/admin/manage/user/user-' + resourceName + '/edit')
        
        cy.get('form#CreateUser')
            .should('be.visible')

        // Associate user with a role
        cy.get('input#rolesKeyword')
            .clear()
            .type('full-role')

        cy.get('fieldset[data-fieldset="roles"] input[data-field^="roles-full-role"]')
            .click()

        // Associate user with a cluster role
        cy.get('input#clusterRolesKeyword')
            .clear()
            .type('full-clusterrole')

        cy.get('fieldset[data-fieldset="clusterroles"] input[data-field^="clusterroles-full-clusterrole"]')
            .click()
        
        // Test Submit form
        cy.get('form#CreateUser button[type="submit"]')
            .click()
        
        cy.get('#notifications .message.show .title')
            .should(($notification) => {
                expect($notification).contain('User "user-' + resourceName + '" updated successfully')
            })
        
        // Test user redirection
        cy.location('pathname')
            .should('eq', '/admin/manage/user/user-' + resourceName)
    });

  })