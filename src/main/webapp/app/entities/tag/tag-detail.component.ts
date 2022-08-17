import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ITag } from 'app/shared/model/tag.model';
import { TagService } from './tag.service';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { ICompetitionProblem } from '../../shared/model/competition-problem.model';
import { ISubmission } from '../../shared/model/submission.model';
import { zip } from 'rxjs';
import { getPointsColor } from 'app/shared/util/points-color';

@Component({
    selector: 'jhi-tag-detail',
    templateUrl: './tag-detail.component.html',
    styleUrls: ['./tag-detail.component.css']
})
export class TagDetailComponent implements OnInit {
    tag: ITag;
    problems: ICompetitionProblem[];
    problemsWithOfficialTag: Set<number> = new Set<number>();
    userTaggedProblems: ICompetitionProblem[];
    submissions: ISubmission[];
    getPointsColor = getPointsColor;

    constructor(protected activatedRoute: ActivatedRoute, private tagService: TagService) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ tag }) => {
            this.tag = tag;
            this.loadData();
        });
    }

    loadData() {
        zip(this.tagService.findProblems(this.tag.id), this.tagService.findProblemsTaggedByUsers(this.tag.id)).subscribe(
            ([officialProblemsRes, userProblemsRes]: HttpResponse<ICompetitionProblem[]>[]) => {
                this.problems = officialProblemsRes.body;
                for (const problem of this.problems) {
                    if (problem.path) {
                        // Remove the root
                        problem.path.shift();
                    }
                    this.problemsWithOfficialTag.add(problem.id);
                }

                this.userTaggedProblems = [];
                for (const problem of userProblemsRes.body) {
                    if (problem.path) {
                        // Remove the root
                        problem.path.shift();
                    }
                    if (!this.problemsWithOfficialTag.has(problem.id)) {
                        this.userTaggedProblems.push(problem);
                    }
                }

                this.sortProblems(this.problems);
                this.sortProblems(this.userTaggedProblems);
            },
            error => this.handleError(error)
        );

        this.tagService.findSubmissions(this.tag.id).subscribe((res: HttpResponse<ICompetitionProblem[]>) => (this.submissions = res.body));
    }

    private handleError(error) {
        console.log(error);
    }

    previousState() {
        window.history.back();
    }

    getPathAtPos(p: ICompetitionProblem, pos: number) {
        if (!p.path) return '';
        while (pos < 0) pos += p.path.length;
        if (pos >= p.path.length) pos %= p.path.length;
        return p.path[pos];
    }

    getGroup(p: ICompetitionProblem) {
        if (!p.path) {
            return 'z';
        }
        for (const comp of p.path) {
            // Group can also have 2 letters, e.g. "AB".
            if (comp.length <= 2) {
                return comp;
            }
        }
        return 'z';
    }

    getYear(p: ICompetitionProblem) {
        if (!p.path) return 'z';
        for (const comp of p.path) {
            if (comp.length == 4) {
                return comp;
            }
        }
        return 'z';
    }

    getComp(p: ICompetitionProblem) {
        if (!p.path) return '';
        for (const comp of p.path) {
            if (comp.length > 4) {
                return comp;
            }
        }
        return 'z';
    }

    compareAt(a: ICompetitionProblem, b: ICompetitionProblem, projector) {
        return projector(a).localeCompare(projector(b));
    }

    sortProblems(problems: ICompetitionProblem[]) {
        problems.sort(
            (a: ICompetitionProblem, b: ICompetitionProblem) =>
                this.compareAt(a, b, this.getGroup) || this.compareAt(a, b, this.getComp) || this.compareAt(a, b, this.getYear)
        );
    }
}
