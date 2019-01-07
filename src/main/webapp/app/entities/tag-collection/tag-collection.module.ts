import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArenaSharedModule } from 'app/shared';
import {
    TagCollectionComponent,
    TagCollectionDetailComponent,
    TagCollectionUpdateComponent,
    TagCollectionDeletePopupComponent,
    TagCollectionDeleteDialogComponent,
    tagCollectionRoute,
    tagCollectionPopupRoute
} from './';

const ENTITY_STATES = [...tagCollectionRoute, ...tagCollectionPopupRoute];

@NgModule({
    imports: [ArenaSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        TagCollectionComponent,
        TagCollectionDetailComponent,
        TagCollectionUpdateComponent,
        TagCollectionDeleteDialogComponent,
        TagCollectionDeletePopupComponent
    ],
    entryComponents: [
        TagCollectionComponent,
        TagCollectionUpdateComponent,
        TagCollectionDeleteDialogComponent,
        TagCollectionDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaTagCollectionModule {}
