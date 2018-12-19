/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { ProblemUpdateComponent } from 'app/entities/problem/problem-update.component';
import { ProblemService } from 'app/entities/problem/problem.service';
import { Problem } from 'app/shared/model/problem.model';

describe('Component Tests', () => {
    describe('Problem Management Update Component', () => {
        let comp: ProblemUpdateComponent;
        let fixture: ComponentFixture<ProblemUpdateComponent>;
        let service: ProblemService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [ProblemUpdateComponent]
            })
                .overrideTemplate(ProblemUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(ProblemUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ProblemService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new Problem(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.problem = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new Problem();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.problem = entity;
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
