import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { TagCollection } from 'app/shared/model/tag-collection.model';
import { TagCollectionService } from './tag-collection.service';
import { TagCollectionComponent } from './tag-collection.component';
import { TagCollectionDetailComponent } from './tag-collection-detail.component';
import { TagCollectionUpdateComponent } from './tag-collection-update.component';
import { TagCollectionDeletePopupComponent } from './tag-collection-delete-dialog.component';
import { ITagCollection } from 'app/shared/model/tag-collection.model';

@Injectable({ providedIn: 'root' })
export class TagCollectionResolve implements Resolve<ITagCollection> {
    constructor(private service: TagCollectionService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<TagCollection> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<TagCollection>) => response.ok),
                map((tagCollection: HttpResponse<TagCollection>) => tagCollection.body)
            );
        }
        return of(new TagCollection());
    }
}

export const tagCollectionRoute: Routes = [
    {
        path: 'tag-collection',
        component: TagCollectionComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollection.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'tag-collection/:id/view',
        component: TagCollectionDetailComponent,
        resolve: {
            tagCollection: TagCollectionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollection.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'tag-collection/new',
        component: TagCollectionUpdateComponent,
        resolve: {
            tagCollection: TagCollectionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollection.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'tag-collection/:id/edit',
        component: TagCollectionUpdateComponent,
        resolve: {
            tagCollection: TagCollectionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollection.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const tagCollectionPopupRoute: Routes = [
    {
        path: 'tag-collection/:id/delete',
        component: TagCollectionDeletePopupComponent,
        resolve: {
            tagCollection: TagCollectionResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollection.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
