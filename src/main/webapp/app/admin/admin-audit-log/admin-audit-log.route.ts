import { Route } from '@angular/router';
import { JhiResolvePagingParams } from 'ng-jhipster';

import { AdminAuditLogComponent } from './admin-audit-log.component';

export const adminAuditLogRoute: Route = {
    path: 'admin-audit-log',
    component: AdminAuditLogComponent,
    resolve: {
        pagingParams: JhiResolvePagingParams
    },
    data: {
        pageTitle: 'adminAuditLog.title',
        defaultSort: 'performedDate,desc'
    }
};
