import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArenaSharedModule } from 'app/shared';
import {
    CompetitionComponent,
    CompetitionDetailComponent,
    CompetitionUpdateComponent,
    CompetitionDeletePopupComponent,
    CompetitionDeleteDialogComponent,
    CompetitionProblemsComponent,
    CompetitionChildrenComponent,
    CompetitionPathComponent,
    CatalogComponent,
    ProblemInCompetitionComponent,
    competitionRoute,
    competitionPopupRoute
} from './';

const ENTITY_STATES = [...competitionRoute, ...competitionPopupRoute];

@NgModule({
    imports: [ArenaSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        CompetitionComponent,
        CompetitionChildrenComponent,
        CompetitionDetailComponent,
        CompetitionUpdateComponent,
        CompetitionDeleteDialogComponent,
        CompetitionDeletePopupComponent,
        CompetitionProblemsComponent,
        CompetitionPathComponent,
        CatalogComponent,
        ProblemInCompetitionComponent
    ],
    entryComponents: [CompetitionComponent, CompetitionUpdateComponent, CompetitionDeleteDialogComponent, CompetitionDeletePopupComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaCompetitionModule {}
