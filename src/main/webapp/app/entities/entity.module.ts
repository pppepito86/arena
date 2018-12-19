import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ArenaCompetitionModule } from './competition/competition.module';
import { ArenaProblemModule } from './problem/problem.module';
import { ArenaCompetitionProblemModule } from './competition-problem/competition-problem.module';
import { ArenaSubmissionModule } from './submission/submission.module';
/* jhipster-needle-add-entity-module-import - JHipster will add entity modules imports here */

@NgModule({
    // prettier-ignore
    imports: [
        ArenaCompetitionModule,
        ArenaProblemModule,
        ArenaCompetitionProblemModule,
        ArenaSubmissionModule,
        /* jhipster-needle-add-entity-module - JHipster will add entity modules here */
    ],
    declarations: [],
    entryComponents: [],
    providers: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaEntityModule {}
