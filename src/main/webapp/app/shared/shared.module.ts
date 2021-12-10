import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { NgbDateAdapter } from '@ng-bootstrap/ng-bootstrap';

import { NgbDateMomentAdapter } from './util/datepicker-adapter';
import {
    ArenaSharedLibsModule,
    ArenaSharedCommonModule,
    JhiLoginModalComponent,
    HasAnyAuthorityDirective,
    HasNotAuthorityDirective
} from './';

import { JhMaterialModule } from 'app/shared/jh-material.module';
@NgModule({
    imports: [JhMaterialModule, ArenaSharedLibsModule, ArenaSharedCommonModule],
    declarations: [JhiLoginModalComponent, HasAnyAuthorityDirective, HasNotAuthorityDirective],
    providers: [{ provide: NgbDateAdapter, useClass: NgbDateMomentAdapter }],
    entryComponents: [JhiLoginModalComponent],
    exports: [JhMaterialModule, ArenaSharedCommonModule, JhiLoginModalComponent, HasAnyAuthorityDirective, HasNotAuthorityDirective],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class ArenaSharedModule {
    static forRoot() {
        return {
            ngModule: ArenaSharedModule
        };
    }
}
