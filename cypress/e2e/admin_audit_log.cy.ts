describe('Admin audit log', () => {
  // The one problem whose package fixture is seeded into the e2e work dir by
  // scripts/dev/run_e2e.sh; changing a limit rewrites grade.properties inside
  // that package, so it only works for a problem that actually has one.
  const problemId = 90;

  const auditRows = () => cy.get('tr[data-testid^="audit-row-"]');

  // cy.login drives the real login modal, so a page has to be loaded first.
  const loginAsAdmin = () => {
    cy.visit('/');
    cy.login('testadmin', 'testpass');
  };

  it('records a time limit change made from the problem edit form', () => {
    loginAsAdmin();
    cy.intercept('PUT', '/api/problems').as('saveProblem');

    cy.visit(`/#/problem/${problemId}/edit`);
    cy.get('#field_time').should('be.visible');

    cy.get('#field_time')
      .invoke('val')
      .then(value => {
        const oldTime = Number(value);
        const newTime = oldTime + 500;

        cy.get('#field_time').clear().type(String(newTime));
        cy.get('#save-entity').click();
        cy.wait('@saveProblem');
        // On success the form calls window.history.back(); let that land
        // first, or it pops the navigation below and lands back on home.
        cy.hash().should('not.contain', 'edit');

        cy.visit('/#/admin/admin-audit-log');
        auditRows()
          .first()
          .within(() => {
            cy.contains('problem.update');
            // The changes cell reads "time: <old> -> <new>".
            cy.contains(String(oldTime));
            cy.contains(String(newTime));
          });
      });
  });

  it('records deactivating and reactivating a user', () => {
    loginAsAdmin();
    cy.intercept('PUT', '/api/users').as('updateUser');

    // Driven from the edit form rather than the list: the list is a paginated
    // Material table whose filter only works once the users have arrived.
    const setActivated = (activated: boolean) => {
      cy.visit('/#/admin/user-management/audittarget/edit');
      // Enabled only once the resolved user is bound to the form.
      cy.get('#activated').should('be.enabled');
      if (activated) {
        cy.get('#activated').check();
      } else {
        cy.get('#activated').uncheck();
      }
      cy.get('form[name=editForm] button[type=submit]').click();
      cy.wait('@updateUser');
      // Same as above: the form navigates back on success.
      cy.hash().should('not.contain', 'edit');
    };

    setActivated(false);
    setActivated(true);

    cy.visit('/#/admin/admin-audit-log');
    // Both toggles are on top of the log; which of the two is first does not
    // matter here, only that each was recorded against the right user.
    auditRows().then($rows => {
      const top = $rows.eq(0).text() + ' ' + $rows.eq(1).text();
      expect(top).to.contain('user.activate');
      expect(top).to.contain('user.deactivate');
      expect(top).to.contain('audittarget');
    });
  });

  it('filters the log by action', () => {
    loginAsAdmin();
    cy.intercept('GET', '/api/admin-audit-log?*').as('loadLog');

    cy.visit('/#/admin/admin-audit-log');
    cy.wait('@loadLog');
    auditRows().should('have.length.greaterThan', 0);

    // Filter by whatever action the newest entry happens to have.
    auditRows()
      .first()
      .find('.badge')
      .invoke('text')
      .then(text => {
        const action = text.trim();
        cy.get('[data-testid=audit-action-filter]').select(action);
        cy.wait('@loadLog');

        auditRows().should('have.length.greaterThan', 0);
        // Synchronous assertions: the table is rebuilt on every load, so a
        // retrying one would chase rows that are no longer in the DOM.
        cy.get('tr[data-testid^="audit-row-"] .badge').each($badge => {
          expect($badge.text().trim()).to.eq(action);
        });
      });
  });

  it('stays out of reach of a logged-in non-admin', () => {
    cy.visit('/');
    cy.login('testuser', 'testpass');

    cy.visit('/#/admin/admin-audit-log');
    // For someone logged in without ROLE_ADMIN the guard only cancels the
    // navigation (see UserRouteAccessService.checkLogin), so the app stays put
    // rather than moving to the access-denied page.
    cy.hash().should('not.contain', 'admin-audit-log');
    cy.get('#admin-audit-log-page-heading').should('not.exist');
  });

  it('sends an anonymous visitor to the access-denied page', () => {
    cy.visit('/#/admin/admin-audit-log');

    cy.hash().should('eq', '#/accessdenied');
    cy.get('#admin-audit-log-page-heading').should('not.exist');
  });

  it('refuses the API without an admin token', () => {
    cy.request({ url: '/api/admin-audit-log', failOnStatusCode: false })
      .its('status')
      .should('be.oneOf', [401, 403]);
  });
});
