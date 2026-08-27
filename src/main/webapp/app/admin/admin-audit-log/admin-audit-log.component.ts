import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ITEMS_PER_PAGE } from 'app/shared';
import { AdminAuditLog, IAuditDetails, IFieldChange } from './admin-audit-log.model';
import { AdminAuditLogService } from './admin-audit-log.service';

@Component({
    selector: 'jhi-admin-audit-log',
    templateUrl: './admin-audit-log.component.html'
})
export class AdminAuditLogComponent implements OnInit, OnDestroy {
    entries: AdminAuditLog[];
    fromDate: string;
    toDate: string;
    adminLogin: string;
    action: string;
    actions: string[] = [];
    admins: string[] = [];
    itemsPerPage: any;
    links: any;
    queryCount: number;
    page: number;
    routeData: any;
    predicate: any;
    previousPage: any;
    reverse: boolean;
    totalItems: number;

    constructor(
        private adminAuditLogService: AdminAuditLogService,
        private alertService: JhiAlertService,
        private parseLinks: JhiParseLinks,
        private activatedRoute: ActivatedRoute,
        private datePipe: DatePipe,
        private router: Router
    ) {
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.routeData = this.activatedRoute.data.subscribe(data => {
            this.page = data['pagingParams'].page;
            this.previousPage = data['pagingParams'].page;
            this.reverse = data['pagingParams'].ascending;
            this.predicate = data['pagingParams'].predicate;
        });
    }

    ngOnInit() {
        this.today();
        this.previousMonth();
        this.loadFilterOptions();
        this.loadAll();
    }

    ngOnDestroy() {
        this.routeData.unsubscribe();
    }

    previousMonth() {
        const dateFormat = 'yyyy-MM-dd';
        let fromDate: Date = new Date();

        if (fromDate.getMonth() === 0) {
            fromDate = new Date(fromDate.getFullYear() - 1, 11, fromDate.getDate());
        } else {
            fromDate = new Date(fromDate.getFullYear(), fromDate.getMonth() - 1, fromDate.getDate());
        }

        this.fromDate = this.datePipe.transform(fromDate, dateFormat);
    }

    today() {
        const dateFormat = 'yyyy-MM-dd';
        const today: Date = new Date();
        this.toDate = this.datePipe.transform(new Date(today.getFullYear(), today.getMonth(), today.getDate()), dateFormat);
    }

    loadFilterOptions() {
        this.adminAuditLogService.actions().subscribe(actions => (this.actions = actions), (res: any) => this.onError(res));
        this.adminAuditLogService.admins().subscribe(admins => (this.admins = admins), (res: any) => this.onError(res));
    }

    loadAll() {
        const req: any = {
            page: this.page - 1,
            size: this.itemsPerPage,
            sort: this.sort(),
            fromDate: this.fromDate,
            toDate: this.toDate
        };
        // createRequestOption stringifies whatever it is given, so only send
        // the filters that are actually set.
        if (this.adminLogin) {
            req.adminLogin = this.adminLogin;
        }
        if (this.action) {
            req.action = this.action;
        }

        this.adminAuditLogService
            .query(req)
            .subscribe(
                (res: HttpResponse<AdminAuditLog[]>) => this.onSuccess(res.body, res.headers),
                (res: HttpResponse<any>) => this.onError(res.body)
            );
    }

    sort() {
        const direction = this.reverse ? 'asc' : 'desc';
        const result = [this.predicate + ',' + direction];
        if (this.predicate !== 'id') {
            // Same direction as the primary sort, so entries sharing a
            // timestamp still come out newest-first.
            result.push('id,' + direction);
        }
        return result;
    }

    loadPage(page: number) {
        if (page !== this.previousPage) {
            this.previousPage = page;
            this.transition();
        }
    }

    transition() {
        this.router.navigate(['/admin/admin-audit-log'], {
            queryParams: {
                page: this.page,
                sort: this.predicate + ',' + (this.reverse ? 'asc' : 'desc')
            }
        });
        this.loadAll();
    }

    /** Reset to the first page whenever a filter changes. */
    filter() {
        this.page = 1;
        this.previousPage = 1;
        this.loadAll();
    }

    changes(entry: AdminAuditLog): IFieldChange[] {
        const details = this.parseDetails(entry);
        return details.changes || [];
    }

    note(entry: AdminAuditLog): string {
        return this.parseDetails(entry).note;
    }

    /** The route to the entity an entry is about, or null when there is none. */
    entityLink(entry: AdminAuditLog): any[] {
        if (!entry.entityId) {
            return null;
        }
        if (entry.entityType === 'problem') {
            return ['/problem', entry.entityId, 'view'];
        }
        if (entry.entityType === 'user' && entry.entityLabel) {
            return ['/admin/user-management', entry.entityLabel, 'view'];
        }
        return null;
    }

    private parseDetails(entry: AdminAuditLog): IAuditDetails {
        if (!entry.details) {
            return {};
        }
        try {
            return JSON.parse(entry.details);
        } catch (e) {
            // A row written by an older version, or hand-edited in the database.
            return { note: entry.details };
        }
    }

    private onSuccess(data, headers) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = headers.get('X-Total-Count');
        this.queryCount = this.totalItems;
        this.entries = data;
    }

    private onError(error) {
        this.alertService.error(error.error, error.message, null);
    }
}
