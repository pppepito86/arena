/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { TagCollectionTagUpdateComponent } from 'app/entities/tag-collection-tag/tag-collection-tag-update.component';
import { TagCollectionTagService } from 'app/entities/tag-collection-tag/tag-collection-tag.service';
import { TagCollectionTag } from 'app/shared/model/tag-collection-tag.model';

describe('Component Tests', () => {
    describe('TagCollectionTag Management Update Component', () => {
        let comp: TagCollectionTagUpdateComponent;
        let fixture: ComponentFixture<TagCollectionTagUpdateComponent>;
        let service: TagCollectionTagService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [TagCollectionTagUpdateComponent]
            })
                .overrideTemplate(TagCollectionTagUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(TagCollectionTagUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(TagCollectionTagService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new TagCollectionTag(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.tagCollectionTag = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new TagCollectionTag();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.tagCollectionTag = entity;
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
