import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ITag } from 'app/shared/model/tag.model';
import { IProblem } from '../../shared/model/problem.model';
import { ISubmission } from '../../shared/model/submission.model';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';

type EntityResponseType = HttpResponse<ITag>;
type EntityArrayResponseType = HttpResponse<ITag[]>;

@Injectable({ providedIn: 'root' })
export class TagService {
    public resourceUrl = SERVER_API_URL + 'api/tags';

    constructor(protected http: HttpClient) {}

    create(tag: ITag): Observable<EntityResponseType> {
        return this.http.post<ITag>(this.resourceUrl, tag, { observe: 'response' });
    }

    update(tag: ITag): Observable<EntityResponseType> {
        return this.http.put<ITag>(this.resourceUrl, tag, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<ITag>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(publicOnly?: boolean, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        let url = this.resourceUrl;
        if (publicOnly === true) {
            url += '?publicOnly=true';
        }
        return this.http.get<ITag[]>(url, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    findProblems(id: number): Observable<HttpResponse<ICompetitionProblem[]>> {
        const url = `${this.resourceUrl}/${id}/problems`;
        return this.http.get<ICompetitionProblem[]>(url, { observe: 'response' });
    }

    findSubmissions(id: number): Observable<HttpResponse<ISubmission[]>> {
        const url = `${this.resourceUrl}/${id}/submissions`;
        return this.http.get<IProblem[]>(url, { observe: 'response' });
    }

    findProblemsTaggedByUsers(id: number): Observable<HttpResponse<ICompetitionProblem[]>> {
        const url = `${this.resourceUrl}/${id}/problems-tagged-by-users`;
        return this.http.get<ICompetitionProblem[]>(url, { observe: 'response' });
    }
}
