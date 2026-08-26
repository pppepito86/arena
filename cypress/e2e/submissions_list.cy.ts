// The submission list pages show only the current user's submissions unless
// they are an admin (SubmissionResource.getAllSubmissions /
// CompetitionResource.getCompetitionSubmissions), so these specs rely on the
// submissions run_e2e.sh seeds for testuser.

const rows = 'tr[data-testid^="submission-row-"]';
const PAGE_SIZE = 20; // ITEMS_PER_PAGE in app/shared/constants/pagination.constants.ts

/**
 * The id cell of the first row on the current page. Returned as a live chain
 * rather than its text, so assertions against it keep retrying while the next
 * page's data is still in flight.
 */
function firstRowIdCell() {
  return cy.get(rows).first().find('[data-testid=submission-cell-id]');
}

/**
 * Clicks a numbered page link inside the ngb-pagination widget. The link's text
 * is the number surrounded by template whitespace, so it can't be matched
 * exactly; anchoring on the trimmed number keeps "2" from also matching "12".
 */
function goToPage(n: number) {
  cy.contains('[data-testid=submissions-pagination] .page-link', new RegExp(`^\\s*${n}\\s*$`)).click();
}

describe('Submissions list', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.login();
  });

  describe('the global list at /submission', () => {
    beforeEach(() => {
      cy.visit('/#/submission');
      cy.get(rows).should('have.length', PAGE_SIZE);
    });

    it('pages through the results', () => {
      cy.get('[data-testid=submissions-pagination]').should('be.visible');

      firstRowIdCell()
        .invoke('text')
        .then(idOnPage1 => {
          goToPage(2);

          cy.hash().should('contain', 'page=2');
          cy.get(rows).should('have.length', PAGE_SIZE);
          firstRowIdCell().should('not.have.text', idOnPage1);

          // ...and back again.
          goToPage(1);
          cy.get(rows).should('have.length', PAGE_SIZE);
          firstRowIdCell().should('have.text', idOnPage1);
        });
    });

    it('re-sorts when the ID header is clicked', () => {
      // The route's defaultSort is id,desc, so the first click flips it to
      // ascending and the newest submission should drop off the first page.
      firstRowIdCell()
        .invoke('text')
        .then(newestId => {
          cy.get('[data-testid=sort-by-id]').click();

          cy.hash().should('contain', 'sort=id,asc');
          cy.get(rows).should('have.length', PAGE_SIZE);
          firstRowIdCell().should('not.have.text', newestId);
          firstRowIdCell()
            .invoke('text')
            .then(oldestId => {
              expect(Number(oldestId)).to.be.lessThan(Number(newestId));
            });
        });
    });
  });

  describe('the per-competition list at /catalog/1/submissions', () => {
    // Competition 1 is the catalog root, so this lists the user's submissions
    // across the whole competition tree.
    beforeEach(() => {
      cy.visit('/#/catalog/1/submissions');
      cy.get(rows).should('have.length', PAGE_SIZE);
    });

    it('pages through the results', () => {
      cy.get('[data-testid=submissions-pagination]').should('be.visible');

      firstRowIdCell()
        .invoke('text')
        .then(idOnPage1 => {
          goToPage(2);

          cy.hash().should('contain', 'page=2');
          cy.hash().should('contain', '/catalog/1/submissions');
          cy.get(rows).should('have.length', PAGE_SIZE);
          firstRowIdCell().should('not.have.text', idOnPage1);

          goToPage(1);
          firstRowIdCell().should('have.text', idOnPage1);
        });
    });
  });
});
