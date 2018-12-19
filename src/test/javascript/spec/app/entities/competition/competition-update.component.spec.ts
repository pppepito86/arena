/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { CompetitionUpdateComponent } from 'app/entities/competition/competition-update.component';
import { CompetitionService } from 'app/entities/competition/competition.service';
import { Competition } from 'app/shared/model/competition.model';

describe('Component Tests', () => {
    describe('Competition Management Update Component', () => {
        let comp: CompetitionUpdateComponent;
        let fixture: ComponentFixture<CompetitionUpdateComponent>;
        let service: CompetitionService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [CompetitionUpdateComponent]
            })
                .overrideTemplate(CompetitionUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(CompetitionUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(CompetitionService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new Competition(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.competition = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new Competition();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.competition = entity;
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
