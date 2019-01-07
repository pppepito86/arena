import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ArenaCompetitionModule } from './competition/competition.module';
import { ArenaProblemModule } from './problem/problem.module';
import { ArenaCompetitionProblemModule } from './competition-problem/competition-problem.module';
import { ArenaSubmissionModule } from './submission/submission.module';
import { ArenaTagCollectionModule } from './tag-collection/tag-collection.module';
import { ArenaTagModule } from './tag/tag.module';
import { ArenaTagCollectionTagModule } from './tag-collection-tag/tag-collection-tag.module';
/* jhipster-needle-add-entity-module-import - JHipster will add entity modules imports here */

@NgModule({
    // prettier-ignore
    imports: [
        ArenaCompetitionModule,
        ArenaProblemModule,
        ArenaCompetitionProblemModule,
        ArenaSubmissionModule,
        ArenaTagCollectionModule,
        ArenaTagModule,
        ArenaTagCollectionTagModule,
        /* jhipster-needle-add-entity-module - JHipster will add entity modules here */
    ],
    declarations: [],
    entryComponents: [],
    providers: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaEntityModule {}
