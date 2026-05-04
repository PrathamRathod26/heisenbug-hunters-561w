import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  DoaMatrixCreateRequest,
  DoaMatrixResponse,
  DoaMatrixUpdateRequest,
  LineOfBusiness
} from '../models/doa.model';
import { RoleName } from '../models/role.model';

@Injectable({ providedIn: 'root' })
export class DoaService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/rbac/doa-matrix`;

  create(body: DoaMatrixCreateRequest): Observable<DoaMatrixResponse> {
    return this.http.post<DoaMatrixResponse>(this.base, body);
  }

  get(id: string): Observable<DoaMatrixResponse> {
    return this.http.get<DoaMatrixResponse>(`${this.base}/${id}`);
  }

  list(roleId?: string): Observable<DoaMatrixResponse[]> {
    let params = new HttpParams();
    if (roleId) params = params.set('roleId', roleId);
    return this.http.get<DoaMatrixResponse[]>(this.base, { params });
  }

  lookup(role: RoleName, lob: LineOfBusiness, geo: string): Observable<DoaMatrixResponse> {
    const params = new HttpParams().set('role', role).set('lob', lob).set('geo', geo);
    return this.http.get<DoaMatrixResponse>(`${this.base}/lookup`, { params });
  }

  update(id: string, body: DoaMatrixUpdateRequest): Observable<DoaMatrixResponse> {
    return this.http.patch<DoaMatrixResponse>(`${this.base}/${id}`, body);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }
}
