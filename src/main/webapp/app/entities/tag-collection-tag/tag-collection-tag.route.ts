import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { TagCollectionTag } from 'app/shared/model/tag-collection-tag.model';
import { TagCollectionTagService } from './tag-collection-tag.service';
import { TagCollectionTagComponent } from './tag-collection-tag.component';
import { TagCollectionTagDetailComponent } from './tag-collection-tag-detail.component';
import { TagCollectionTagUpdateComponent } from './tag-collection-tag-update.component';
import { TagCollectionTagDeletePopupComponent } from './tag-collection-tag-delete-dialog.component';
import { ITagCollectionTag } from 'app/shared/model/tag-collection-tag.model';

@Injectable({ providedIn: 'root' })
export class TagCollectionTagResolve implements Resolve<ITagCollectionTag> {
    constructor(private service: TagCollectionTagService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<TagCollectionTag> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<TagCollectionTag>) => response.ok),
                map((tagCollectionTag: HttpResponse<TagCollectionTag>) => tagCollectionTag.body)
            );
        }
        return of(new TagCollectionTag());
    }
}

export const tagCollectionTagRoute: Routes = [
    {
        path: 'tag-collection-tag',
        component: TagCollectionTagComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollectionTag.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'tag-collection-tag/:id/view',
        component: TagCollectionTagDetailComponent,
        resolve: {
            tagCollectionTag: TagCollectionTagResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollectionTag.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'tag-collection-tag/new',
        component: TagCollectionTagUpdateComponent,
        resolve: {
            tagCollectionTag: TagCollectionTagResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollectionTag.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'tag-collection-tag/:id/edit',
        component: TagCollectionTagUpdateComponent,
        resolve: {
            tagCollectionTag: TagCollectionTagResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollectionTag.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const tagCollectionTagPopupRoute: Routes = [
    {
        path: 'tag-collection-tag/:id/delete',
        component: TagCollectionTagDeletePopupComponent,
        resolve: {
            tagCollectionTag: TagCollectionTagResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'arenaApp.tagCollectionTag.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
