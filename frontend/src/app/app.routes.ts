import { Routes } from '@angular/router';
import { authGuard, personaGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },

  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent),
    title: 'Sign in'
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./features/forbidden/forbidden.component').then(m => m.ForbiddenComponent),
    title: 'Not authorised'
  },

  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    title: 'Dashboard'
  },
  {
    path: 'claims',
    canActivate: [authGuard],
    loadChildren: () => import('./features/claims/claims.routes').then(m => m.CLAIMS_ROUTES)
  },

  {
    path: 'rbac/users',
    canActivate: [authGuard, personaGuard(['ADMIN'])],
    loadChildren: () => import('./features/rbac/users/users.routes').then(m => m.USERS_ROUTES)
  },
  {
    path: 'rbac/roles',
    canActivate: [authGuard, personaGuard(['ADMIN'])],
    loadChildren: () => import('./features/rbac/roles/roles.routes').then(m => m.ROLES_ROUTES)
  },
  {
    path: 'rbac/permissions',
    canActivate: [authGuard, personaGuard(['ADMIN'])],
    loadChildren: () =>
      import('./features/rbac/permissions/permissions.routes').then(m => m.PERMISSIONS_ROUTES)
  },
  {
    path: 'rbac/doa-matrix',
    canActivate: [authGuard, personaGuard(['ADMIN'])],
    loadChildren: () => import('./features/rbac/doa/doa.routes').then(m => m.DOA_ROUTES)
  },

  { path: '**', redirectTo: 'dashboard' }
];
