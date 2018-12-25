import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ICompetition } from 'app/shared/model/competition.model';
import { IProblem } from '../../shared/model/problem.model';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { IUserPoints } from '../../shared/model/user-points.model';

type EntityResponseType = HttpResponse<ICompetition>;
type EntityArrayResponseType = HttpResponse<ICompetition[]>;

@Injectable({ providedIn: 'root' })
export class CompetitionService {
    public resourceUrl = SERVER_API_URL + 'api/competitions';

    constructor(protected http: HttpClient) {}

    create(competition: ICompetition): Observable<EntityResponseType> {
        return this.http.post<ICompetition>(this.resourceUrl, competition, { observe: 'response' });
    }

    update(competition: ICompetition): Observable<EntityResponseType> {
        return this.http.put<ICompetition>(this.resourceUrl, competition, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<ICompetition>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<ICompetition[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    findChildren(id: number, req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<ICompetition[]>(`${this.resourceUrl}/${id}/children`, { params: options, observe: 'response' });
    }

    findProblems(id: number, req?: any): Observable<HttpResponse<ICompetitionProblem[]>> {
        const options = createRequestOption(req);
        return this.http.get<ICompetition[]>(`${this.resourceUrl}/${id}/problems`, { params: options, observe: 'response' });
    }

    findPath(id: number): Observable<EntityArrayResponseType> {
        return this.http.get<ICompetition[]>(`${this.resourceUrl}/${id}/path`, { observe: 'response' });
    }

    findProblem(id: number, competitionProblemId: number): Observable<HttpResponse<IProblem>> {
        return this.http.get<IProblem>(`${this.resourceUrl}/${id}/problem/${competitionProblemId}`, { observe: 'response' });
    }

    submitSolution(competitionId: number, competitionProblemId: number, solution: string) {
        const url = `${this.resourceUrl}/${competitionId}/problem/${competitionProblemId}/submit`;
        return this.http.post<IProblem>(url, solution, { observe: 'response' });
    }

    getStandings(competitionId: number, req?: any): Observable<HttpResponse<IUserPoints[]>> {
        const options = createRequestOption(req);
        const url = `${this.resourceUrl}/${competitionId}/standings`;
        return this.http.get<IUserPoints[]>(url, { params: options, observe: 'response' });
    }
}
