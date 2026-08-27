import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { createRequestOption } from 'app/shared';
import { SERVER_API_URL } from 'app/app.constants';
import { AdminAuditLog } from './admin-audit-log.model';

@Injectable({ providedIn: 'root' })
export class AdminAuditLogService {
    private resourceUrl = SERVER_API_URL + 'api/admin-audit-log';

    constructor(private http: HttpClient) {}

    query(req: any): Observable<HttpResponse<AdminAuditLog[]>> {
        const params: HttpParams = createRequestOption(req);
        return this.http.get<AdminAuditLog[]>(this.resourceUrl, {
            params,
            observe: 'response'
        });
    }

    actions(): Observable<string[]> {
        return this.http.get<string[]>(`${this.resourceUrl}/actions`);
    }

    admins(): Observable<string[]> {
        return this.http.get<string[]>(`${this.resourceUrl}/admins`);
    }
}
