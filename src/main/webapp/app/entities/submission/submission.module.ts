import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArenaSharedModule } from 'app/shared';
import { ArenaAdminModule } from 'app/admin/admin.module';
import {
    SubmissionComponent,
    SubmissionDetailComponent,
    SubmissionUpdateComponent,
    SubmissionDeletePopupComponent,
    SubmissionDeleteDialogComponent,
    submissionRoute,
    submissionPopupRoute
} from './';

const ENTITY_STATES = [...submissionRoute, ...submissionPopupRoute];

@NgModule({
    imports: [ArenaSharedModule, ArenaAdminModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        SubmissionComponent,
        SubmissionDetailComponent,
        SubmissionUpdateComponent,
        SubmissionDeleteDialogComponent,
        SubmissionDeletePopupComponent
    ],
    entryComponents: [SubmissionComponent, SubmissionUpdateComponent, SubmissionDeleteDialogComponent, SubmissionDeletePopupComponent],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaSubmissionModule {}
