import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JhiAlertService } from 'ng-jhipster';

import { IProblem } from 'app/shared/model/problem.model';
import { IProblemPackageInfo } from 'app/shared/model/problem-package-info.model';
import { ProblemService } from './problem.service';
import { ITagCollection } from 'app/shared/model/tag-collection.model';
import { TagCollectionService } from 'app/entities/tag-collection';

@Component({
    selector: 'jhi-problem-update',
    templateUrl: './problem-update.component.html'
})
export class ProblemUpdateComponent implements OnInit {
    problem: IProblem;
    isSaving: boolean;
    isUploading = false;
    fileUploadStatus: string;
    editableLimits = true;
    packageInfo: IProblemPackageInfo = null;
    statementCacheBuster = Date.now();
    private packageInfoRequestSeq = 0;

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected problemService: ProblemService,
        protected tagCollectionService: TagCollectionService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ problem }) => {
            this.problem = problem;
            if (this.problem && this.problem.id !== undefined) {
                this.loadPackageInfo(this.problem.id);
            }
        });
        this.activatedRoute.queryParams.subscribe(params => {
            this.editableLimits =
                params.editableLimits === undefined || !['0', 'false'].includes(String(params.editableLimits).toLowerCase());
        });
    }

    protected loadPackageInfo(problemId: number) {
        const reqSeq = ++this.packageInfoRequestSeq;
        this.problemService.getPackageInfo(problemId).subscribe(
            (res: HttpResponse<IProblemPackageInfo>) => {
                if (reqSeq === this.packageInfoRequestSeq) {
                    this.packageInfo = res.body;
                }
            },
            () => {
                if (reqSeq === this.packageInfoRequestSeq) {
                    this.packageInfo = null;
                }
            }
        );
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        // If the memory and time are not editable, don't send
        // them back to the server to prevent them from overriding
        // the values from a new zip that has just been uploaded.
        if (!this.editableLimits) {
            this.problem.memory = null;
            this.problem.time = null;
        }
        if (this.problem.id !== undefined) {
            this.subscribeToSaveResponse(this.problemService.update(this.problem));
        } else {
            this.subscribeToSaveResponse(this.problemService.create(this.problem));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IProblem>>) {
        result.subscribe((res: HttpResponse<IProblem>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    trackTagCollectionById(index: number, item: ITagCollection) {
        return item.id;
    }

    uploadFile(input: HTMLInputElement) {
        if (this.isUploading) {
            return;
        }
        const files = input.files;
        if (files && files.length > 0) {
            const file = files[0];
            input.value = '';
            const reqSeq = ++this.packageInfoRequestSeq;
            this.isUploading = true;
            this.fileUploadStatus = '(uploading...)';
            this.problemService.uploadFile(this.problem.id, file).subscribe(
                (info: IProblemPackageInfo) => {
                    this.isUploading = false;
                    this.fileUploadStatus = '(done)';
                    if (reqSeq !== this.packageInfoRequestSeq) {
                        return;
                    }
                    this.packageInfo = info;
                    this.statementCacheBuster = Date.now();
                    if (info) {
                        if (info.timeMs != null) {
                            this.problem.time = info.timeMs;
                        }
                        if (info.memoryMb != null) {
                            this.problem.memory = info.memoryMb;
                        }
                    }
                },
                () => {
                    this.isUploading = false;
                    this.fileUploadStatus = '(error)';
                }
            );
        }
    }
}
