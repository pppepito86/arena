/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { ArenaTestModule } from '../../../test.module';
import { CompetitionDetailComponent } from 'app/entities/competition/competition-detail.component';
import { Competition } from 'app/shared/model/competition.model';

describe('Component Tests', () => {
    describe('Competition Management Detail Component', () => {
        let comp: CompetitionDetailComponent;
        let fixture: ComponentFixture<CompetitionDetailComponent>;
        const route = ({ data: of({ competition: new Competition(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [ArenaTestModule],
                declarations: [CompetitionDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(CompetitionDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(CompetitionDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.competition).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
