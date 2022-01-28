import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { IProblem } from 'app/shared/model/problem.model';
import { AccountService } from 'app/core';

import { NUM_FETCH_ITEMS } from 'app/shared';
import { ProblemService } from './problem.service';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { MatSort } from '@angular/material/sort';

@Component({
    selector: 'jhi-problem',
    templateUrl: './problem.component.html'
})
export class ProblemComponent implements OnInit, OnDestroy {
    currentAccount: any;
    problems: IProblem[];
    error: any;
    success: any;
    eventSubscriber: Subscription;
    routeData: any;
    links: any;
    totalItems: any;
    queryCount: any;
    predicate: any;
    reverse: any;
    displayedColumns: string[] = ['id', 'title', 'competition', 'year', 'group', 'version', 'tags', 'actions'];
    dataSource: any;
    @ViewChild(MatPaginator) paginator: MatPaginator;
    @ViewChild(MatSort) sort: MatSort;

    constructor(
        protected problemService: ProblemService,
        protected parseLinks: JhiParseLinks,
        protected jhiAlertService: JhiAlertService,
        protected accountService: AccountService,
        protected activatedRoute: ActivatedRoute,
        protected router: Router,
        protected eventManager: JhiEventManager
    ) {
        this.routeData = this.activatedRoute.data.subscribe(data => {
            this.reverse = data.pagingParams.ascending;
            this.predicate = data.pagingParams.predicate;
        });
    }

    loadAll() {
        this.problemService
            .query({
                size: NUM_FETCH_ITEMS
            })
            .subscribe(
                (res: HttpResponse<IProblem[]>) => {
                    this.dataSource = new MatTableDataSource(res.body);
                    setTimeout(() => {
                        this.dataSource.filterPredicate = this.filterFunction;
                        this.dataSource.paginator = this.paginator;
                        this.dataSource.sort = this.sort;
                    });
                },
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    ngOnInit() {
        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.registerChangeInProblems();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: IProblem) {
        return item.id;
    }

    registerChangeInProblems() {
        this.eventSubscriber = this.eventManager.subscribe('problemListModification', response => this.loadAll());
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
    applyFilter(event: Event) {
        const filterValue = (event.target as HTMLInputElement).value;
        this.dataSource.filter = filterValue.trim().toLowerCase();
    }

    filterFunction(problem: any, filter: string) {
        let tagsStr = '';
        if (problem.allTags) {
            for (let tag of problem.allTags) {
                tagsStr += '|' + tag.title;
            }
        }
        tagsStr = tagsStr.toLowerCase();
        let problemStr =
            problem.id + '|' + problem.title + '|' + problem.competitionLabel + '|' + problem.year + '|' + problem.group + '|' + tagsStr;
        problemStr = problemStr.toLowerCase();
        const filterParts = filter.split(/\s+/);
        for (const part of filterParts) {
            const ind = part.indexOf(':');
            if (ind > 0) {
                const key = part.substring(0, ind);
                let val = part.substring(ind + 1);
                if (key.startsWith('g') || key.startsWith('г')) {
                    val = val.toLowerCase();
                    if (val === 'а') val = 'a';
                    if (val === 'б' || val === 'в') val = 'b';
                    if (val === 'с' || val === 'ц') val = 'c';
                    if (val === 'д') val = 'd';
                    if (val === 'е') val = 'e';
                    if (!problem.group || !problem.group.toLowerCase().includes(val)) {
                        return false;
                    }
                } else if (key.startsWith('t') || key.startsWith('т')) {
                    if (!tagsStr.includes(val)) {
                        return false;
                    }
                } else if (key.startsWith('c') || key.startsWith('с')) {
                    if (!problem.competitionLabel.toLowerCase().includes(val)) {
                        return false;
                    }
                } else if (key.startsWith('v') || key.startsWith('в')) {
                    if (problem.version != val) {
                        return false;
                    }
                }
            } else if (!problemStr.includes(part)) {
                return false;
            }
        }
        return true;
    }
}
