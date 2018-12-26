import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ICompetition } from 'app/shared/model/competition.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { CompetitionService } from './competition.service';

@Component({
    selector: 'jhi-competition-path',
    templateUrl: './competition-path.component.html'
})
export class CompetitionPathComponent implements OnInit {
    currentAccount: any;
    error: any;
    success: any;
    routeData: any;
    parentCompetition: ICompetition;
    path: ICompetition[];

    constructor(
        protected competitionService: CompetitionService,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
        protected activatedRoute: ActivatedRoute,
        protected router: Router,
        protected eventManager: JhiEventManager
    ) {
        this.router.routeReuseStrategy.shouldReuseRoute = function() {
            return false;
        };
        // this.routeData = this.activatedRoute.data.subscribe(data => {
        //     this.parentCompetition = data.parentCompetition;
        // });
    }

    loadAll() {
        const competitionId = this.activatedRoute.snapshot.params['id'];
        this.competitionService
            .findPath(competitionId)
            .subscribe(
                (res: HttpResponse<ICompetition[]>) => (this.path = res.body),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    ngOnInit() {
        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
