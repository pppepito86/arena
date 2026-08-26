
// Navigates the catalog down to a problem. Each step asserts on the breadcrumb
// before the next click, since the catalog page only actually swaps in the new
// competition's data asynchronously after the route change.
function browseToProblem(path: string[], problem: string) {
  cy.get('[data-testid=nav-catalog]').click();
  cy.get('[data-testid=breadcrumb]').should('contain', 'Задачи');

  path.forEach(label => {
    cy.get(`[data-testid="competition-link-${label}"]`).click();
    cy.get('[data-testid=breadcrumb]').should('contain', label);
  });

  cy.get(`[data-testid="competition-link-${problem}"], [data-testid="problem-link-${problem}"]`).click();
}

// 'sign' (Есенен турнир > 2012 > E) ships a real problem archive plus a
// known-correct reference solution, so grading can actually run end to end
// (unlike 'bridges', which has no archive on disk).
const SIGN = { path: ['Есенен турнир', '2012', 'E'], problem: 'sign' };

/** Pastes a fixture into the solution box and submits it. */
function submitFixture(fixture: string) {
  cy.fixture(fixture).then(solutionCode => {
    cy.get('#solution').invoke('val', solutionCode).trigger('input');
    cy.get('[data-testid=solution-form] #save-entity').click();
  });
}

describe('Submit Solution', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('unregistered user cannot submit', () => {
    // 1. Make sure user is not logged in.
    cy.get('#account-menu').click();
    cy.get('#login').should('be.visible');

    // 2. Select a problem from catalog.
    browseToProblem(['Есенен турнир', '2017', 'B'], 'bridges');

    cy.get('[data-testid=solution-form]').should('not.exist');
  });

  it('logged user submits a solution and it is judged', () => {
    cy.login();

    browseToProblem(SIGN.path, SIGN.problem);
    cy.get('[data-testid=solution-form]').should('be.visible');

    submitFixture('sign-solution.cpp');

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
        cy.get('[data-testid=submission-cell-verdict]').should('contain', 'OK');
      });
    });
  });

  it('reports a wrong solution as not accepted', () => {
    cy.login();

    browseToProblem(SIGN.path, SIGN.problem);
    cy.get('[data-testid=solution-form]').should('be.visible');

    // Same problem, but a solution that compiles and terminates quickly while
    // printing a constant, so every test case should come back WA.
    submitFixture('sign-wrong-solution.cpp');

    cy.hash().should('match', /^#\/submission\/\d+\/view/);
    cy.get('[data-testid=judging-banner]', { timeout: 60000 }).should('not.exist');

    // The overall verdict is the per-test verdicts joined with commas, so a
    // fully wrong solution must not report a single accepted test.
    cy.get('[data-testid=submission-verdict]')
      .invoke('text')
      .then(verdict => {
        expect(verdict).to.contain('WA');
        expect(verdict.split(',').map(v => v.trim())).to.not.include('OK');
      });
    cy.get('[data-testid=submission-points]')
      .invoke('text')
      .then(points => expect(points.trim()).to.eq('0'));

    cy.url().then(url => {
      const submissionId = url.match(/\/submission\/(\d+)\/view/)[1];

      cy.contains('a', 'sign', { matchCase: false }).click();
      cy.get('[data-testid=view-submissions]').click();

      cy.get(`[data-testid="submission-row-${submissionId}"]`).within(() => {
        cy.get('[data-testid=submission-cell-verdict]').should('contain', 'WA');
      });
    });
  });
});
