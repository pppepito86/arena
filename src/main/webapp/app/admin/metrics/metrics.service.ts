import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { IStats, IStatsQueue } from './metrics.model';

@Injectable({ providedIn: 'root' })
export class JhiMetricsService {
    constructor(private http: HttpClient) {}

    getMetrics(): Observable<any> {
        return this.http.get(SERVER_API_URL + 'management/metrics');
    }

    getStats(): Observable<IStats> {
        return this.http.get<IStats>(SERVER_API_URL + 'management/stats');
    }

    getQueue(): Observable<IStatsQueue> {
        return this.http.get<IStatsQueue>(SERVER_API_URL + 'management/stats/queue');
    }

    sendMail(): Observable<any> {
        return this.http.post(SERVER_API_URL + 'management/debug/send-mail', {});
    }

    threadDump(): Observable<any> {
        return this.http.get(SERVER_API_URL + 'management/threaddump');
    }
}
