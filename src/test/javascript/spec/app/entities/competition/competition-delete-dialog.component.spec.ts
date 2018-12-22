/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArenaTestModule } from '../../../test.module';
import { CompetitionDeleteDialogComponent } from 'app/entities/competition/competition-delete-dialog.component';
import { CompetitionService } from 'app/entities/competition/competition.service';

describe('Component Tests', () => {
    describe('Competition Management Delete Component', () => {
        let comp: CompetitionDeleteDialogComponent;
        let fixture: ComponentFixture<CompetitionDeleteDialogComponent>;
        let service: CompetitionService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [CompetitionDeleteDialogComponent]
            })
                .overrideTemplate(CompetitionDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(CompetitionDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(CompetitionService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('confirmDelete', () => {
            it('Should call delete service on confirmDelete', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    spyOn(service, 'delete').and.returnValue(of({}));

                    // WHEN
                    comp.confirmDelete(123);
                    tick();

                    // THEN
                    expect(service.delete).toHaveBeenCalledWith(123);
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalled();
                })
            ));
        });
    });
});
