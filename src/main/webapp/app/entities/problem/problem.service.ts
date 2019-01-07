import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IProblem } from 'app/shared/model/problem.model';
import { ITag } from 'app/shared/model/tag.model';

type EntityResponseType = HttpResponse<IProblem>;
type EntityArrayResponseType = HttpResponse<IProblem[]>;

@Injectable({ providedIn: 'root' })
export class ProblemService {
    public resourceUrl = SERVER_API_URL + 'api/problems';

    constructor(protected http: HttpClient) {}

    create(problem: IProblem): Observable<EntityResponseType> {
        return this.http.post<IProblem>(this.resourceUrl, problem, { observe: 'response' });
    }

    update(problem: IProblem): Observable<EntityResponseType> {
        return this.http.put<IProblem>(this.resourceUrl, problem, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<IProblem>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<IProblem[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    getTags(problemId: number): Observable<HttpResponse<ITag[]>> {
        const url = `${this.resourceUrl}/${problemId}/tags`;
        return this.http.get<ITag[]>(url, { observe: 'response' });
    }

    updateTags(problemId: number, tags: ITag[]): Observable<HttpResponse<any>> {
        let tagsCopy: ITag[] = [];
        for (let tag of tags) {
            let tagCopy = {
                id: tag.id,
                title: tag.title
            };
            if (typeof tagCopy.id === 'string') {
                tagCopy.id = null;
            }
            tagsCopy.push(tagCopy);
        }

        const url = `${this.resourceUrl}/${problemId}/tags`;
        return this.http.post(url, tagsCopy, { observe: 'response' });
    }
}
