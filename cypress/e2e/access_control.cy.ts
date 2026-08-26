describe('Access control', () => {
  // /submission is a plain ROLE_USER route guarded by UserRouteAccessService,
  // which is the same guard every protected route in the app uses, so it stands
  // in for all of them.
  const protectedRoute = '#/submission';

  it('sends an anonymous visitor to the access-denied page and opens the login modal', () => {
    cy.visit(`/${protectedRoute}`);

    cy.hash().should('eq', '#/accessdenied');
    cy.get('[data-testid=error-403]').should('be.visible');
    // The guard also pops the login modal so the visitor can authenticate.
    cy.get('[data-testid=login-submit]').should('be.visible');
  });

  it('lets the visitor in after logging in from that modal', () => {
    cy.visit(`/${protectedRoute}`);
    cy.get('[data-testid=login-submit]').should('be.visible');

    cy.get('#username').type('testuser');
    cy.get('#password').type('testpass');
    cy.get('[data-testid=login-submit]').click();

    cy.visit(`/${protectedRoute}`);
    cy.hash().should('contain', '#/submission');
    cy.get('[data-testid=error-403]').should('not.exist');
    cy.get('tr[data-testid^="submission-row-"]').should('have.length.greaterThan', 0);
  });

  it('sends the visitor back to access-denied after logging out', () => {
    cy.visit('/');
    cy.login();

    cy.visit(`/${protectedRoute}`);
    cy.hash().should('contain', '#/submission');

    cy.get('#account-menu').click();
    cy.get('#logout').click();

    cy.visit(`/${protectedRoute}`);
    cy.hash().should('eq', '#/accessdenied');
    cy.get('[data-testid=error-403]').should('be.visible');
  });
});
