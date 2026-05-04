import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, map, of, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/common.model';
import { UserResponse, UserSummaryResponse } from '../models/user.model';
import { RoleName } from '../models/role.model';

export type Persona = 'POLICYHOLDER' | 'ADJUSTER' | 'ADMIN';

const SESSION_KEY = 'claims.session.v1';

const PERSONA_FOR_ROLE: Record<RoleName, Persona | null> = {
  POLICYHOLDER:       'POLICYHOLDER',
  ADJUSTER:           'ADJUSTER',
  SR_ADJUSTER:        'ADJUSTER',
  ADMIN:              'ADMIN',
  FRAUD_INVESTIGATOR: null,
  SURVEYOR:           null,
  GRIEVANCE_OFFICER:  null,
  AML_OFFICER:        null,
  FINANCE_APPROVER:   null,
  AUDITOR:            null,
  SYSTEM:             null
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly _user = signal<UserResponse | null>(this.loadFromStorage());

  readonly user = this._user.asReadonly();
  readonly isAuthenticated = computed(() => this._user() !== null);

  readonly activeRoles = computed<RoleName[]>(() => {
    const u = this._user();
    if (!u) return [];
    return u.roleAssignments
      .filter(a => a.status === 'ACTIVE')
      .map(a => a.roleName);
  });

  readonly persona = computed<Persona | null>(() => {
    const roles = this.activeRoles();
    if (roles.includes('ADMIN'))        return 'ADMIN';
    if (roles.includes('SR_ADJUSTER'))  return 'ADJUSTER';
    if (roles.includes('ADJUSTER'))     return 'ADJUSTER';
    if (roles.includes('POLICYHOLDER')) return 'POLICYHOLDER';
    return null;
  });

  listLoginCandidates(): Observable<UserSummaryResponse[]> {
    const params = new HttpParams().set('size', 50).set('sort', 'email');
    return this.http
      .get<Page<UserSummaryResponse>>(`${environment.apiBaseUrl}/api/v1/rbac/users`, { params })
      .pipe(map(p => p.content));
  }

  login(userId: string): Observable<UserResponse | null> {
    return this.http.get<UserResponse>(`${environment.apiBaseUrl}/api/v1/rbac/users/${userId}`).pipe(
      tap(u => {
        if (!this.isAllowed(u)) {
          this._user.set(null);
          localStorage.removeItem(SESSION_KEY);
          return;
        }
        this._user.set(u);
        localStorage.setItem(SESSION_KEY, JSON.stringify(u));
      }),
      map(u => (this.isAllowed(u) ? u : null))
    );
  }

  refresh(): Observable<UserResponse | null> {
    const u = this._user();
    if (!u) return of(null);
    return this.login(u.id);
  }

  logout(): void {
    this._user.set(null);
    localStorage.removeItem(SESSION_KEY);
    this.router.navigate(['/login']);
  }

  isAllowed(u: UserResponse): boolean {
    return u.roleAssignments
      .filter(a => a.status === 'ACTIVE')
      .some(a => PERSONA_FOR_ROLE[a.roleName] !== null);
  }

  hasPersona(p: Persona): boolean {
    return this.persona() === p;
  }

  private loadFromStorage(): UserResponse | null {
    if (typeof localStorage === 'undefined') return null;
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserResponse;
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
