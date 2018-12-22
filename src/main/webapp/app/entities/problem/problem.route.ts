import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { JhiPaginationUtil, JhiResolvePagingParams } from 'ng-jhipster';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Problem } from 'app/shared/model/problem.model';
import { ProblemService } from './problem.service';
import { ProblemComponent } from './problem.component';
import { ProblemDetailComponent } from './problem-detail.component';
import { ProblemUpdateComponent } from './problem-update.component';
import { ProblemDeletePopupComponent } from './problem-delete-dialog.component';
import { IProblem } from 'app/shared/model/problem.model';

@Injectable({ providedIn: 'root' })
export class ProblemResolve implements Resolve<IProblem> {
    constructor(private service: ProblemService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<Problem> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<Problem>) => response.ok),
                map((problem: HttpResponse<Problem>) => problem.body)
            );
        }
        return of(new Problem());
    }
}

export const problemRoute: Routes = [
    {
        path: 'problem',
        component: ProblemComponent,
        resolve: {
            pagingParams: JhiResolvePagingParams
        },
        data: {
            authorities: ['ROLE_USER'],
            defaultSort: 'id,asc',
            pageTitle: 'arenaApp.problem.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'problem/:id/view',
        component: ProblemDetailComponent,
        resolve: {
            problem: ProblemResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.problem.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'problem/new',
        component: ProblemUpdateComponent,
        resolve: {
            problem: ProblemResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.problem.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'problem/:id/edit',
        component: ProblemUpdateComponent,
        resolve: {
            problem: ProblemResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.problem.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const problemPopupRoute: Routes = [
    {
        path: 'problem/:id/delete',
        component: ProblemDeletePopupComponent,
        resolve: {
            problem: ProblemResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.problem.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
