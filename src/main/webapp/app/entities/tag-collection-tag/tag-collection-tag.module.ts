import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

import { ArenaSharedModule } from 'app/shared';
import {
    TagCollectionTagComponent,
    TagCollectionTagDetailComponent,
    TagCollectionTagUpdateComponent,
    TagCollectionTagDeletePopupComponent,
    TagCollectionTagDeleteDialogComponent,
    tagCollectionTagRoute,
    tagCollectionTagPopupRoute
} from './';

const ENTITY_STATES = [...tagCollectionTagRoute, ...tagCollectionTagPopupRoute];

@NgModule({
    imports: [ArenaSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        TagCollectionTagComponent,
        TagCollectionTagDetailComponent,
        TagCollectionTagUpdateComponent,
        TagCollectionTagDeleteDialogComponent,
        TagCollectionTagDeletePopupComponent
    ],
    entryComponents: [
        TagCollectionTagComponent,
        TagCollectionTagUpdateComponent,
        TagCollectionTagDeleteDialogComponent,
        TagCollectionTagDeletePopupComponent
    ],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaTagCollectionTagModule {}
