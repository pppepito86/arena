import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ITagCollectionTag } from 'app/shared/model/tag-collection-tag.model';

type EntityResponseType = HttpResponse<ITagCollectionTag>;
type EntityArrayResponseType = HttpResponse<ITagCollectionTag[]>;

@Injectable({ providedIn: 'root' })
export class TagCollectionTagService {
    public resourceUrl = SERVER_API_URL + 'api/tag-collection-tags';

    constructor(protected http: HttpClient) {}

    create(tagCollectionTag: ITagCollectionTag): Observable<EntityResponseType> {
        return this.http.post<ITagCollectionTag>(this.resourceUrl, tagCollectionTag, { observe: 'response' });
    }

    update(tagCollectionTag: ITagCollectionTag): Observable<EntityResponseType> {
        return this.http.put<ITagCollectionTag>(this.resourceUrl, tagCollectionTag, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<ITagCollectionTag>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<ITagCollectionTag[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
