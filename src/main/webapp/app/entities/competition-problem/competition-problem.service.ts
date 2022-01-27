import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ICompetitionProblem } from 'app/shared/model/competition-problem.model';

type EntityResponseType = HttpResponse<ICompetitionProblem>;
type EntityArrayResponseType = HttpResponse<ICompetitionProblem[]>;

@Injectable({ providedIn: 'root' })
export class CompetitionProblemService {
    public resourceUrl = SERVER_API_URL + 'api/competition-problems';

    constructor(protected http: HttpClient) {}

    create(competitionProblem: ICompetitionProblem): Observable<EntityResponseType> {
        return this.http.post<ICompetitionProblem>(this.resourceUrl, competitionProblem, { observe: 'response' });
    }

    update(competitionProblem: ICompetitionProblem): Observable<EntityResponseType> {
        return this.http.put<ICompetitionProblem>(this.resourceUrl, competitionProblem, { observe: 'response' });
    }

    find(id: number): Observable<HttpResponse<ICompetitionProblem>> {
        return this.http.get<ICompetitionProblem>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<ICompetitionProblem[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    submitAuthorSolution(competitionProblemId: number) {
        return this.http.get(`${this.resourceUrl}/${competitionProblemId}/author`);
    }

    autoSetTimeLimit(competitionProblemId: number) {
        return this.http.get(`${this.resourceUrl}/${competitionProblemId}/times?set=true`);
    }
}
