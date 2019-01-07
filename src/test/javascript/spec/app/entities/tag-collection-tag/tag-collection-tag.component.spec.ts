/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of } from 'rxjs';
import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { ArenaTestModule } from '../../../test.module';
import { TagCollectionTagComponent } from 'app/entities/tag-collection-tag/tag-collection-tag.component';
import { TagCollectionTagService } from 'app/entities/tag-collection-tag/tag-collection-tag.service';
import { TagCollectionTag } from 'app/shared/model/tag-collection-tag.model';

describe('Component Tests', () => {
    describe('TagCollectionTag Management Component', () => {
        let comp: TagCollectionTagComponent;
        let fixture: ComponentFixture<TagCollectionTagComponent>;
        let service: TagCollectionTagService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [TagCollectionTagComponent],
                providers: []
            })
                .overrideTemplate(TagCollectionTagComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(TagCollectionTagComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(TagCollectionTagService);
        });

        it('Should call load all on init', () => {
            // GIVEN
            const headers = new HttpHeaders().append('link', 'link;link');
            spyOn(service, 'query').and.returnValue(
                of(
                    new HttpResponse({
                        body: [new TagCollectionTag(123)],
                        headers
                    })
                )
            );

            // WHEN
            comp.ngOnInit();

            // THEN
            expect(service.query).toHaveBeenCalled();
            expect(comp.tagCollectionTags[0]).toEqual(jasmine.objectContaining({ id: 123 }));
        });
    });
});
