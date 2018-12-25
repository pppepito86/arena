import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Competition } from 'app/shared/model/competition.model';
import { CompetitionService } from './competition.service';
import { CompetitionComponent } from './competition.component';
import { CompetitionDetailComponent } from './competition-detail.component';
import { CompetitionUpdateComponent } from './competition-update.component';
import { CompetitionDeletePopupComponent } from './competition-delete-dialog.component';
import { ICompetition } from 'app/shared/model/competition.model';
import { CompetitionChildrenComponent } from './competition-children.component';
import { CatalogComponent } from './catalog.component';
import { ProblemInCompetitionComponent } from './problem-in-competition.component';
import { StandingsComponent } from './standings.component';
import { SubmissionComponent } from '../submission';

@Injectable({ providedIn: 'root' })
export class CompetitionResolve implements Resolve<ICompetition> {
    constructor(private service: CompetitionService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Competition> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<Competition>) => response.ok),
                map((competition: HttpResponse<Competition>) => competition.body)
            );
        }
        return of(new Competition());
    }
}

export const competitionRoute: Routes = [
    {
        path: 'competition',
        component: CompetitionComponent,
        resolve: {
            pagingParams: JhiResolvePagingParams
        },
        data: {
            authorities: ['ROLE_USER'],
            defaultSort: 'id,asc',
            pageTitle: 'arenaApp.competition.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'catalog',
        redirectTo: 'catalog/1',
        pathMatch: 'full'
    },
    {
        path: 'catalog/:id',
        component: CatalogComponent,
        resolve: {
            pagingParams: JhiResolvePagingParams,
            parentCompetition: CompetitionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            defaultSort: 'id,asc',
            pageTitle: 'arenaApp.competition.home.title'
        },
        canActivate: [UserRouteAccessService],
        runGuardsAndResolvers: 'always'
    },
    {
        path: 'catalog/:id/submissions',
        component: SubmissionComponent,
        resolve: {
            pagingParams: JhiResolvePagingParams
        },
        data: {
            authorities: ['ROLE_USER'],
            defaultSort: 'id,desc',
            pageTitle: 'arenaApp.submission.home.title',
            forCompetition: true
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'catalog/:id/problem/:compProb',
        component: ProblemInCompetitionComponent,
        resolve: {
            competition: CompetitionResolve
        },
        data: {
            authorities: ['ROLE_USER']
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'catalog/:id/problem/:compProb/submissions',
        component: SubmissionComponent,
        resolve: {
            pagingParams: JhiResolvePagingParams
        },
        data: {
            authorities: ['ROLE_USER'],
            defaultSort: 'id,desc',
            pageTitle: 'arenaApp.submission.home.title',
            forProblem: true
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'competition/:id/view',
        component: CompetitionDetailComponent,
        resolve: {
            competition: CompetitionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.competition.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'competition/new',
        component: CompetitionUpdateComponent,
        resolve: {
            competition: CompetitionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.competition.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'competition/:id/edit',
        component: CompetitionUpdateComponent,
        resolve: {
            competition: CompetitionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.competition.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'standings',
        redirectTo: 'catalog/1/standings',
        pathMatch: 'full'
    },
    {
        path: 'catalog/:id/standings',
        component: StandingsComponent,
        resolve: {
            parentCompetition: CompetitionResolve,
            pagingParams: JhiResolvePagingParams
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.standings.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const competitionPopupRoute: Routes = [
    {
        path: 'competition/:id/delete',
        component: CompetitionDeletePopupComponent,
        resolve: {
            competition: CompetitionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.competition.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
