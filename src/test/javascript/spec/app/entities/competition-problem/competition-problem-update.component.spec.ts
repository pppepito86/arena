/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { CompetitionProblemUpdateComponent } from 'app/entities/competition-problem/competition-problem-update.component';
import { CompetitionProblemService } from 'app/entities/competition-problem/competition-problem.service';
import { CompetitionProblem } from 'app/shared/model/competition-problem.model';

describe('Component Tests', () => {
    describe('CompetitionProblem Management Update Component', () => {
        let comp: CompetitionProblemUpdateComponent;
        let fixture: ComponentFixture<CompetitionProblemUpdateComponent>;
        let service: CompetitionProblemService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [CompetitionProblemUpdateComponent]
            })
                .overrideTemplate(CompetitionProblemUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(CompetitionProblemUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(CompetitionProblemService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new CompetitionProblem(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.competitionProblem = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new CompetitionProblem();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.competitionProblem = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.create).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));
        });
    });
});
