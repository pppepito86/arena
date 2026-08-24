
describe('Submit Solution', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('unregistered user cannot submit', () => {
    // 1. Make sure user is not logged in.
    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');

    // 2. Select a problem from catalog. Each step asserts on the breadcrumb
    // before the next click, since the catalog page only actually swaps in
    // the new competition's data asynchronously after the route change.
    cy.get('[data-testid=nav-catalog]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Задачи');
    cy.get('[data-testid="competition-link-Есенен турнир"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Есенен турнир');
    cy.get('[data-testid="competition-link-2017"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', '2017');
    cy.get('[data-testid="competition-link-B"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'B');
    cy.get('[data-testid="competition-link-bridges"], [data-testid="problem-link-bridges"]').click();

    cy.get('[data-testid=solution-form]').should('not.exist');
  });

  it('logged user submits a solution and it is judged', () => {
    // 1. Log in
    cy.get('#account-menu').click();
    cy.get('#login').click();

    cy.get('#username').type('testuser');
    cy.get('#password').type('testpass');
    cy.get('[data-testid=login-submit]').click();

    // Verify login success
    cy.get('#account-menu').click();
    cy.get('#logout').should('be.visible');

    // 2. Browse to problem 'sign' (Есенен турнир > 2012 > E), which ships a real
    // problem archive + a known-correct reference solution, so grading can
    // actually run end to end (unlike 'bridges', which has no archive on disk).
    cy.get('[data-testid=nav-catalog]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Задачи');
    cy.get('[data-testid="competition-link-Есенен турнир"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'Есенен турнир');
    cy.get('[data-testid="competition-link-2012"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', '2012');
    cy.get('[data-testid="competition-link-E"]').click();
    cy.get('[data-testid=breadcrumb]').should('contain', 'E');
    cy.get('[data-testid="competition-link-sign"], [data-testid="problem-link-sign"]').click();

    cy.get('[data-testid=solution-form]').should('be.visible');

    cy.fixture('sign-solution.cpp').then(solutionCode => {
      cy.get('#solution').invoke('val', solutionCode).trigger('input');
      cy.get('[data-testid=solution-form] #save-entity').click();

      // After submission, it navigates to the submission view page. The app
      // uses hash-based routing, so the route lives in the hash, not the path.
      cy.hash().should('match', /^#\/submission\/\d+\/view/);
      cy.get('h2').invoke('text').should('match', /Submission|Решение/i);
      // cy.contains() normalizes whitespace, which breaks an exact match
      // against a multi-line <pre> block, so check its raw text instead.
      cy.get('pre').invoke('text').should('contain', 'Author: Galina Momcheva');

      // Wait for the async grading (arena -> worker -> isolate sandbox) to
      // actually finish, rather than asserting on an intermediate state.
      cy.get('[data-testid=judging-banner]', { timeout: 60000 }).should('not.exist');
      cy.get('[data-testid=submission-verdict]').should('contain', 'OK');
      cy.get('[data-testid=submission-points]').should('contain', '100');

      // 3. Verify the submission shows up correctly in the submissions list.
      cy.url().then(url => {
        const submissionId = url.match(/\/submission\/(\d+)\/view/)[1];

        cy.contains('a', 'sign', { matchCase: false }).click();
        cy.get('[data-testid=view-submissions]').click();

        cy.get(`[data-testid="submission-row-${submissionId}"]`).within(() => {
          cy.get('[data-testid=submission-row-verdict]').should('contain', 'OK');
        });
      });
    });
  });
});
