import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ITagCollection } from 'app/shared/model/tag-collection.model';

type EntityResponseType = HttpResponse<ITagCollection>;
type EntityArrayResponseType = HttpResponse<ITagCollection[]>;

@Injectable({ providedIn: 'root' })
export class TagCollectionService {
    public resourceUrl = SERVER_API_URL + 'api/tag-collections';

    constructor(protected http: HttpClient) {}

    create(tagCollection: ITagCollection): Observable<EntityResponseType> {
        return this.http.post<ITagCollection>(this.resourceUrl, tagCollection, { observe: 'response' });
    }

    update(tagCollection: ITagCollection): Observable<EntityResponseType> {
        return this.http.put<ITagCollection>(this.resourceUrl, tagCollection, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<ITagCollection>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<ITagCollection[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
