import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ICompetition } from 'app/shared/model/competition.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { CompetitionService } from './competition.service';
import { CompetitionProblemService } from '../competition-problem/competition-problem.service';

@Component({
    styleUrls: ['././competition-children.component.css'],
    selector: 'jhi-competition-children',
    templateUrl: './competition-children.component.html'
})
export class CompetitionChildrenComponent implements OnInit, OnDestroy {
    currentAccount: any;
    competitions: ICompetition[];
    error: any;
    success: any;
    eventSubscriber: Subscription;
    routeData: any;
    links: any;
    totalItems: any;
    queryCount: any;
    itemsPerPage: any;
    page: any;
    previousPage: any;
    parentCompetition: ICompetition;
    grandChildrenCompetitions = {};
    grandChildrenProblems = {};
    DEFAULT_SORT = 'order,asc';

    constructor(
        protected competitionService: CompetitionService,
        protected competitionProblemService: CompetitionProblemService,
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
            this.page = data.pagingParams.page;
            this.previousPage = data.pagingParams.page;
        });
    }

    loadAll() {
        this.competitionService
            .findChildren(this.parentCompetition.id, {
                page: this.page - 1,
                size: this.itemsPerPage,
                sort: [this.DEFAULT_SORT]
            })
            .subscribe(
                (res: HttpResponse<ICompetition[]>) => {
                    this.paginateCompetitions(res.body, res.headers);
                    for (const child of res.body) {
                        this.loadGrandChildrenCompetitions(child);
                        this.loadGrandChildrenProblems(child);
                    }
                },
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    loadGrandChildrenCompetitions(child: any) {
        this.competitionService
            .findChildren(child.id, {
                size: this.itemsPerPage
            })
            .subscribe(
                (res: HttpResponse<ICompetition[]>) => {
                    this.grandChildrenCompetitions[child.id] = res.body;
                    this.grandChildrenCompetitions[child.id].sort(function(a, b) {
                        return a.order - b.order;
                    });
                },
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    loadGrandChildrenProblems(child: any) {
        this.competitionService
            .findProblems(child.id, {
                size: this.itemsPerPage
            })
            .subscribe(
                (res: HttpResponse<ICompetition[]>) => {
                    this.grandChildrenProblems[child.id] = res.body;
                    this.grandChildrenProblems[child.id].sort(function(a, b) {
                        return a.order - b.order;
                    });
                },
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    loadPage(page: number) {
        if (page !== this.previousPage) {
            this.previousPage = page;
            this.transition();
        }
    }

    transition() {
        this.router.navigate(['/catalog'], {
            queryParams: {
                page: this.page,
                size: this.itemsPerPage,
                sort: this.DEFAULT_SORT
            }
        });
        this.loadAll();
    }

    clear() {
        this.page = 0;
        this.router.navigate([
            '/catalog',
            {
                page: this.page,
                sort: this.DEFAULT_SORT
            }
        ]);
        this.loadAll();
    }

    ngOnInit() {
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

    getColor(ratio) {
        return ratio === 1
            ? 'hsl(91 94% 61%)'
            : ratio > 0.9
            ? 'hsl(82deg 95% 61%)'
            : ratio > 0.8
            ? 'hsl(74deg 96% 61%)'
            : ratio > 0.7
            ? 'hsl(66deg 97% 61%)'
            : ratio > 0.6
            ? 'hsl(58deg 99% 62%)'
            : ratio > 0.5
            ? 'hsl(52deg 100% 66%)'
            : ratio > 0.4
            ? 'hsl(44deg 100% 69%)'
            : ratio > 0.3
            ? 'hsl(33deg 100% 71%)'
            : ratio > 0.2
            ? 'hsl(24deg 100% 73%)'
            : ratio > 0.1
            ? 'hsl(15deg 100% 74%)'
            : ratio > 0
            ? 'hsl(7deg 100% 76%)'
            : 'transparent';
        //   ratio === 0 ? 'hsl(0deg 100% 78%)' :
        //     '#FFFFFF';
    }

    protected paginateCompetitions(data: ICompetition[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.queryCount = this.totalItems;
        this.competitions = data;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
