import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { CompetitionProblem } from 'app/shared/model/competition-problem.model';
import { CompetitionProblemService } from './competition-problem.service';
import { CompetitionProblemComponent } from './competition-problem.component';
import { CompetitionProblemDetailComponent } from './competition-problem-detail.component';
import { CompetitionProblemUpdateComponent } from './competition-problem-update.component';
import { CompetitionProblemDeletePopupComponent } from './competition-problem-delete-dialog.component';
import { ICompetitionProblem } from 'app/shared/model/competition-problem.model';

@Injectable({ providedIn: 'root' })
export class CompetitionProblemResolve implements Resolve<ICompetitionProblem> {
    constructor(private service: CompetitionProblemService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<CompetitionProblem> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<CompetitionProblem>) => response.ok),
                map((competitionProblem: HttpResponse<CompetitionProblem>) => competitionProblem.body)
            );
        }
        return of(new CompetitionProblem());
    }
}

export const competitionProblemRoute: Routes = [
    {
        path: 'competition-problem',
        component: CompetitionProblemComponent,
        resolve: {
            pagingParams: JhiResolvePagingParams
        },
        data: {
            authorities: ['ROLE_USER'],
            defaultSort: 'id,asc',
            pageTitle: 'arenaApp.competitionProblem.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'competition-problem/:id/view',
        component: CompetitionProblemDetailComponent,
        resolve: {
            competitionProblem: CompetitionProblemResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.competitionProblem.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'competition-problem/new',
        component: CompetitionProblemUpdateComponent,
        resolve: {
            competitionProblem: CompetitionProblemResolve
        },
        data: {
            authorities: ['ROLE_ADMIN'],
            pageTitle: 'arenaApp.competitionProblem.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'competition-problem/:id/edit',
        component: CompetitionProblemUpdateComponent,
        resolve: {
            competitionProblem: CompetitionProblemResolve
        },
        data: {
            authorities: ['ROLE_ADMIN'],
            pageTitle: 'arenaApp.competitionProblem.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const competitionProblemPopupRoute: Routes = [
    {
        path: 'competition-problem/:id/delete',
        component: CompetitionProblemDeletePopupComponent,
        resolve: {
            competitionProblem: CompetitionProblemResolve
        },
        data: {
            authorities: ['ROLE_ADMIN'],
            pageTitle: 'arenaApp.competitionProblem.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
