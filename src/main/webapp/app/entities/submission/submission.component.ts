import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ISubmission } from 'app/shared/model/submission.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { SubmissionService } from './submission.service';

@Component({
    selector: 'jhi-submission',
    templateUrl: './submission.component.html'
})
export class SubmissionComponent implements OnInit, OnDestroy {
    currentAccount: any;
    submissions: ISubmission[];
    error: any;
    success: any;
    eventSubscriber: Subscription;
    routeData: any;
    links: any;
    totalItems: any;
    queryCount: any;
    itemsPerPage: any;
    page: any;
    predicate: any;
    previousPage: any;
    reverse: any;
    forCompetition: boolean;
    forProblem: boolean;
    competitionId: number;
    problemId: number;

    constructor(
        protected submissionService: SubmissionService,
        protected parseLinks: JhiParseLinks,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
        protected activatedRoute: ActivatedRoute,
        protected router: Router,
        protected eventManager: JhiEventManager
    ) {
        this.itemsPerPage = ITEMS_PER_PAGE;

        this.activatedRoute.queryParams.subscribe(params => {
            this.page = params['page'];
            if (!this.page) {
                this.page = 0;
            }
        });

        this.routeData = this.activatedRoute.data.subscribe(data => {
            this.previousPage = data.pagingParams.page;
            this.reverse = data.pagingParams.ascending;
            this.predicate = data.pagingParams.predicate;
        });
    }

    loadAll() {
        let query;
        if (this.forCompetition) {
            this.competitionId = this.activatedRoute.snapshot.params['id'];
            query = this.submissionService.queryForCompetition(this.competitionId, {
                page: this.page - 1,
                size: this.itemsPerPage,
                sort: this.sort()
            });
        } else if (this.forProblem) {
            this.competitionId = this.activatedRoute.snapshot.params['id'];
            this.problemId = this.activatedRoute.snapshot.params['compProb'];
            query = this.submissionService.queryForProblem(this.competitionId, this.problemId, {
                page: this.page - 1,
                size: this.itemsPerPage,
                sort: this.sort()
            });
        } else {
            query = this.submissionService.query({
                page: this.page - 1,
                size: this.itemsPerPage,
                sort: this.sort()
            });
        }

        query.subscribe(
            (res: HttpResponse<ISubmission[]>) => this.paginateSubmissions(res.body, res.headers),
            (res: HttpErrorResponse) => this.onError(res.message)
        );
    }

    getUrl(): any[] {
        if (this.forCompetition) {
            return ['/catalog', this.competitionId, 'submissions'];
        } else if (this.forProblem) {
            return ['/catalog', this.competitionId, 'problem', this.problemId, 'submissions'];
        } else {
            return ['/submission'];
        }
    }

    loadPage(page: number) {
        if (page !== this.previousPage) {
            this.previousPage = page;
            this.transition();
        }
    }

    transition() {
        console.log('transition', this.page);
        this.router.navigate(this.getUrl(), {
            queryParams: {
                page: this.page,
                size: this.itemsPerPage,
                sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc')
            }
        });
        this.loadAll();
    }

    clear() {
        this.page = 0;
        this.router.navigate(this.getUrl(), {
            queryParams: {
                page: this.page,
                sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc')
            }
        });
        this.loadAll();
    }

    ngOnInit() {
        if (this.activatedRoute.snapshot.data['forCompetition']) {
            this.forCompetition = true;
        }

        if (this.activatedRoute.snapshot.data['forProblem']) {
            this.forProblem = true;
        }

        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInSubmissions();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: ISubmission) {
        return item.id;
    }

    registerChangeInSubmissions() {
        this.eventSubscriber = this.eventManager.subscribe('submissionListModification', response => {
            this.loadAll();
        });
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    protected paginateSubmissions(data: ISubmission[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.queryCount = this.totalItems;
        this.submissions = data;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
