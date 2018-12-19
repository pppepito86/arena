/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArenaTestModule } from '../../../test.module';
import { ProblemDeleteDialogComponent } from 'app/entities/problem/problem-delete-dialog.component';
import { ProblemService } from 'app/entities/problem/problem.service';

describe('Component Tests', () => {
    describe('Problem Management Delete Component', () => {
        let comp: ProblemDeleteDialogComponent;
        let fixture: ComponentFixture<ProblemDeleteDialogComponent>;
        let service: ProblemService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [ProblemDeleteDialogComponent]
            })
                .overrideTemplate(ProblemDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(ProblemDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ProblemService);
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
