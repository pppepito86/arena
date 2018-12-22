/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { CompetitionProblemDetailComponent } from 'app/entities/competition-problem/competition-problem-detail.component';
import { CompetitionProblem } from 'app/shared/model/competition-problem.model';

describe('Component Tests', () => {
    describe('CompetitionProblem Management Detail Component', () => {
        let comp: CompetitionProblemDetailComponent;
        let fixture: ComponentFixture<CompetitionProblemDetailComponent>;
        const route = ({ data: of({ competitionProblem: new CompetitionProblem(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [CompetitionProblemDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(CompetitionProblemDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(CompetitionProblemDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.competitionProblem).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
