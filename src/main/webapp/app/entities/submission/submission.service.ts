import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ISubmission } from 'app/shared/model/submission.model';

type EntityResponseType = HttpResponse<ISubmission>;
type EntityArrayResponseType = HttpResponse<ISubmission[]>;

@Injectable({ providedIn: 'root' })
export class SubmissionService {
    public resourceUrl = SERVER_API_URL + 'api/submissions';

    constructor(protected http: HttpClient) {}

    create(submission: ISubmission): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(submission);
        return this.http
            .post<ISubmission>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(submission: ISubmission): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(submission);
        return this.http
            .put<ISubmission>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<ISubmission>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<ISubmission[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    queryForCompetition(competitionId: number, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        const url = `api/competitions/${competitionId}/submissions`;
        return this.http
            .get<ISubmission[]>(url, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    queryForProblem(competitionId: number, problemId: number, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        const url = `api/competitions/${competitionId}/problem/${problemId}/submissions`;
        return this.http
            .get<ISubmission[]>(url, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    protected convertDateFromClient(submission: ISubmission): ISubmission {
        const copy: ISubmission = Object.assign({}, submission, {
            uploadDate: submission.uploadDate != null && submission.uploadDate.isValid() ? submission.uploadDate.toJSON() : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.uploadDate = res.body.uploadDate != null ? moment(res.body.uploadDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((submission: ISubmission) => {
                submission.uploadDate = submission.uploadDate != null ? moment(submission.uploadDate) : null;
            });
        }
        return res;
    }
}
