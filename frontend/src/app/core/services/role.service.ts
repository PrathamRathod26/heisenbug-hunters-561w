import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  RoleCreateRequest,
  RoleName,
  RolePermissionUpdateRequest,
  RoleResponse,
  RoleUpdateRequest
} from '../models/role.model';

@Injectable({ providedIn: 'root' })
export class RoleService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/rbac/roles`;

  create(body: RoleCreateRequest): Observable<RoleResponse> {
    return this.http.post<RoleResponse>(this.base, body);
  }

  list(includeInactive = false): Observable<RoleResponse[]> {
    const params = new HttpParams().set('includeInactive', includeInactive);
    return this.http.get<RoleResponse[]>(this.base, { params });
  }

  get(id: string): Observable<RoleResponse> {
    return this.http.get<RoleResponse>(`${this.base}/${id}`);
  }

  getByName(name: RoleName): Observable<RoleResponse> {
    return this.http.get<RoleResponse>(`${this.base}/by-name/${name}`);
  }

  update(id: string, body: RoleUpdateRequest): Observable<RoleResponse> {
    return this.http.patch<RoleResponse>(`${this.base}/${id}`, body);
  }

  replacePermissions(id: string, body: RolePermissionUpdateRequest): Observable<RoleResponse> {
    return this.http.put<RoleResponse>(`${this.base}/${id}/permissions`, body);
  }
}
