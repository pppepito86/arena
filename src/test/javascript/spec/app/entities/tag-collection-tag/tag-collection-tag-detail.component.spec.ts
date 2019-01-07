/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { TagCollectionTagDetailComponent } from 'app/entities/tag-collection-tag/tag-collection-tag-detail.component';
import { TagCollectionTag } from 'app/shared/model/tag-collection-tag.model';

describe('Component Tests', () => {
    describe('TagCollectionTag Management Detail Component', () => {
        let comp: TagCollectionTagDetailComponent;
        let fixture: ComponentFixture<TagCollectionTagDetailComponent>;
        const route = ({ data: of({ tagCollectionTag: new TagCollectionTag(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [TagCollectionTagDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(TagCollectionTagDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(TagCollectionTagDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.tagCollectionTag).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
