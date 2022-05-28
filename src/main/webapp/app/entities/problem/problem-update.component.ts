import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { JhiAlertService } from 'ng-jhipster';

import { IProblem } from 'app/shared/model/problem.model';
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
    fileUploadStatus: string;
    editableLimits = true;

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
        });
        this.activatedRoute.queryParams.subscribe(params => {
            if (params.editableLimits !== undefined) {
                this.editableLimits = params.editableLimits;
            }
        });
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

    uploadFile(files: FileList) {
        if (files.length > 0) {
            this.fileUploadStatus = '(uploading...)';
            this.problemService
                .uploadFile(this.problem.id, files[0])
                .subscribe(value => (this.fileUploadStatus = '(done)'), error => (this.fileUploadStatus = '(error)'));
        }
    }
}
