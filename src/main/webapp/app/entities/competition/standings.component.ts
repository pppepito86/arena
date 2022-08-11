import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ICompetition } from 'app/shared/model/competition.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { CompetitionService } from './competition.service';
import { IUserPoints } from '../../shared/model/user-points.model';
import { IProblem } from 'app/shared/model/problem.model';
import { ɵangular_packages_platform_browser_dynamic_platform_browser_dynamic_a } from '@angular/platform-browser-dynamic';
import { getPointsColor } from 'app/shared/util/points-color';

interface ProblemColumn {
    id: number;
    competitionId?: number;
    name?: string;
}

@Component({
    selector: 'jhi-standings',
    templateUrl: './standings.component.html'
})
export class StandingsComponent implements OnInit, OnDestroy {
    currentAccount: any;
    scores: IUserPoints[];
    error: any;
    success: any;
    eventSubscriber: Subscription;
    routeData: any;
    links: any;
    totalItems: any;
    queryCount: any;
    itemsPerPage: any;
    page: any = 1;
    predicate: any;
    previousPage: any;
    reverse: any;
    parentCompetition: ICompetition;
    loading = false;
    weeks: number = null;
    filter: string[] = [];

    myPoints?: IUserPoints;
    currentUserIsInStandings = false;
    problems: ProblemColumn[] = [];
    getPointsColor = getPointsColor;

    constructor(
        protected competitionService: CompetitionService,
        protected parseLinks: JhiParseLinks,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
        protected activatedRoute: ActivatedRoute,
        protected router: Router,
        protected eventManager: JhiEventManager
    ) {
        this.router.routeReuseStrategy.shouldReuseRoute = function() {
            return false;
        };
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.routeData = this.activatedRoute.data.subscribe(data => {
            this.parentCompetition = data.parentCompetition;
            console.log('route data, page', this.page, this.activatedRoute);
        });

        this.activatedRoute.queryParams.subscribe((params: Params) => {
            this.weeks = params['w'];
            this.filter = params['f'];
            if (params['page']) {
                this.page = params['page'];
                this.previousPage = this.page;
            }
        });

        this.competitionService.getMyPoints(this.parentCompetition.id).subscribe(
            (res: HttpResponse<IUserPoints>) => {
                this.myPoints = res.body;
                this.processStandingsRow(this.myPoints);
                this.checkIfCurrUserIsInStandings();
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    loadAll() {
        const params = {
            page: this.page - 1,
            size: this.itemsPerPage
        };
        if (this.weeks) {
            params['w'] = this.weeks;
        }
        if (this.filter) {
            params['f'] = this.filter;
        }
        this.competitionService.getStandings(this.parentCompetition.id, params).subscribe(
            (res: HttpResponse<IUserPoints[]>) => {
                const standings = res.body;
                this.processStandings(standings);
                this.paginateStandings(standings, res.headers);
            },
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    loadPage(page: number) {
        this.page = page;
        if (page !== this.previousPage) {
            this.previousPage = page;
            this.transition();
        }
    }

    getParams() {
        const params = {
            page: this.page,
            size: this.itemsPerPage
        };
        if (this.weeks) {
            params['w'] = this.weeks;
        }
        if (this.filter) {
            params['f'] = this.filter;
        }
        return params;
    }

    setWeeks(weeks: number) {
        this.weeks = weeks;
        this.router.navigate([`/catalog/${this.parentCompetition.id}/standings`], {
            queryParams: this.getParams()
        });
    }

    transition() {
        this.router.navigate([`/catalog/${this.parentCompetition.id}/standings`], {
            queryParams: this.getParams()
        });
    }

    ngOnInit() {
        this.loading = true;
        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInCompetitions();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: ICompetition) {
        return item.id;
    }

    registerChangeInCompetitions() {
        this.eventSubscriber = this.eventManager.subscribe('competitionListModification', response => this.loadAll());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    getFontWeight(userId: number): string {
        if (this.myPoints && userId === this.myPoints.userId) {
            return '800'; // very bold
        }
        return 'unset';
    }
    getPointsForProblem(userPoints: IUserPoints, problemId: number) {
        if (!userPoints.perProblem) return 0;

        return userPoints.perProblem[problemId] || 0;
    }
    protected processStandingsRow(row: IUserPoints) {
        if (row.perProblemJson) {
            row.perProblem = JSON.parse(row.perProblemJson);
            row.perProblemJson = null;
        }
    }
    protected processStandings(standings: IUserPoints[]) {
        const problems = new Set<number>();
        for (const row of standings) {
            this.processStandingsRow(row);
            if (row.perProblem) {
                for (const problemId of Object.keys(row.perProblem)) {
                    problems.add(Number(problemId));
                }
            }
        }
        for (const problemId of Array.from(problems)) {
            const problem: ProblemColumn = {
                id: problemId
            };

            this.competitionService.findProblem(/*competitionId*/ 0, problemId).subscribe(
                (res: HttpResponse<IProblem>) => {
                    const p: IProblem = res.body;
                    problem.name = p.title;
                    problem.competitionId = p.competitionId;
                },
                (res: HttpErrorResponse) => this.onError(res.message)
            );
            this.problems.push(problem);
        }
    }

    protected paginateStandings(data: IUserPoints[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.queryCount = this.totalItems;
        this.scores = data;
        this.loading = false;
        this.checkIfCurrUserIsInStandings();
    }

    protected checkIfCurrUserIsInStandings() {
        if (this.myPoints && this.scores) {
            this.currentUserIsInStandings = this.scores.map(row => row.userId).includes(this.myPoints.userId);
        }
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
        this.loading = false;
    }
}
