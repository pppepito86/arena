/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { ArenaTestModule } from '../../../test.module';
import { TagCollectionDeleteDialogComponent } from 'app/entities/tag-collection/tag-collection-delete-dialog.component';
import { TagCollectionService } from 'app/entities/tag-collection/tag-collection.service';

describe('Component Tests', () => {
    describe('TagCollection Management Delete Component', () => {
        let comp: TagCollectionDeleteDialogComponent;
        let fixture: ComponentFixture<TagCollectionDeleteDialogComponent>;
        let service: TagCollectionService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [TagCollectionDeleteDialogComponent]
            })
                .overrideTemplate(TagCollectionDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(TagCollectionDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(TagCollectionService);
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
