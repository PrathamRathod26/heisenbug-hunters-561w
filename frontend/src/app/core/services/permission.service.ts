import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CapabilityCode, PermissionResponse } from '../models/permission.model';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/rbac/permissions`;

  list(capability?: CapabilityCode): Observable<PermissionResponse[]> {
    let params = new HttpParams();
    if (capability) params = params.set('capability', capability);
    return this.http.get<PermissionResponse[]>(this.base, { params });
  }

  get(id: string): Observable<PermissionResponse> {
    return this.http.get<PermissionResponse>(`${this.base}/${id}`);
  }
}
