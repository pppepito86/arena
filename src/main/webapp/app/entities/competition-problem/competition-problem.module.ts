import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArenaSharedModule } from 'app/shared';
import {
    CompetitionProblemComponent,
    CompetitionProblemDetailComponent,
    CompetitionProblemUpdateComponent,
    CompetitionProblemDeletePopupComponent,
    CompetitionProblemDeleteDialogComponent,
    competitionProblemRoute,
    competitionProblemPopupRoute
} from './';

const ENTITY_STATES = [...competitionProblemRoute, ...competitionProblemPopupRoute];

@NgModule({
    imports: [ArenaSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        CompetitionProblemComponent,
        CompetitionProblemDetailComponent,
        CompetitionProblemUpdateComponent,
        CompetitionProblemDeleteDialogComponent,
        CompetitionProblemDeletePopupComponent
    ],
    entryComponents: [
        CompetitionProblemComponent,
        CompetitionProblemUpdateComponent,
        CompetitionProblemDeleteDialogComponent,
        CompetitionProblemDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaCompetitionProblemModule {}
