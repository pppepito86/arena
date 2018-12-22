/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { ProblemDetailComponent } from 'app/entities/problem/problem-detail.component';
import { Problem } from 'app/shared/model/problem.model';

describe('Component Tests', () => {
    describe('Problem Management Detail Component', () => {
        let comp: ProblemDetailComponent;
        let fixture: ComponentFixture<ProblemDetailComponent>;
        const route = ({ data: of({ problem: new Problem(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [ProblemDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(ProblemDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(ProblemDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.problem).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
