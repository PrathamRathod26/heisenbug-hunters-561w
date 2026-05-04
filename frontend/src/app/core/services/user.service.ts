import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/common.model';
import { RoleName } from '../models/role.model';
import {
  EffectivePermissionsResponse,
  UserCreateRequest,
  UserResponse,
  UserRoleAssignRequest,
  UserRoleAssignmentResponse,
  UserStatus,
  UserSummaryResponse,
  UserType,
  UserUpdateRequest
} from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/rbac/users`;

  create(body: UserCreateRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(this.base, body);
  }

  get(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/${id}`);
  }

  update(id: string, body: UserUpdateRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.base}/${id}`, body);
  }

  list(opts: { type?: UserType; status?: UserStatus; page?: number; size?: number; sort?: string } = {})
      : Observable<Page<UserSummaryResponse>> {
    let params = new HttpParams()
      .set('page', opts.page ?? 0)
      .set('size', opts.size ?? 25)
      .set('sort', opts.sort ?? 'email');
    if (opts.type)   params = params.set('type',   opts.type);
    if (opts.status) params = params.set('status', opts.status);
    return this.http.get<Page<UserSummaryResponse>>(this.base, { params });
  }

  assignRole(userId: string, body: UserRoleAssignRequest): Observable<UserRoleAssignmentResponse> {
    return this.http.post<UserRoleAssignmentResponse>(`${this.base}/${userId}/role-assignments`, body);
  }

  listActiveAssignments(userId: string): Observable<UserRoleAssignmentResponse[]> {
    return this.http.get<UserRoleAssignmentResponse[]>(`${this.base}/${userId}/role-assignments`);
  }

  revokeRole(assignmentId: string, revokedBy?: string): Observable<UserRoleAssignmentResponse> {
    let params = new HttpParams();
    if (revokedBy) params = params.set('revokedBy', revokedBy);
    return this.http.delete<UserRoleAssignmentResponse>(
      `${this.base}/role-assignments/${assignmentId}`,
      { params }
    );
  }

  effectivePermissions(userId: string): Observable<EffectivePermissionsResponse> {
    return this.http.get<EffectivePermissionsResponse>(`${this.base}/${userId}/effective-permissions`);
  }

  activeUsersPerRole(): Observable<Record<RoleName, number>> {
    return this.http.get<Record<RoleName, number>>(`${this.base}/stats/active-users-per-role`);
  }
}
