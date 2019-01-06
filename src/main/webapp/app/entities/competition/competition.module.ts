import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { TagInputModule } from 'ngx-chips';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

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
    StandingsComponent,
    competitionRoute,
    competitionPopupRoute
} from './';

const ENTITY_STATES = [...competitionRoute, ...competitionPopupRoute];

@NgModule({
    imports: [
        ArenaSharedModule,
        TagInputModule,
        BrowserAnimationsModule,
        FormsModule,
        ReactiveFormsModule,
        RouterModule.forChild(ENTITY_STATES)
    ],
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
        ProblemInCompetitionComponent,
        StandingsComponent
    ],
    entryComponents: [CompetitionComponent, CompetitionUpdateComponent, CompetitionDeleteDialogComponent, CompetitionDeletePopupComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaCompetitionModule {}
