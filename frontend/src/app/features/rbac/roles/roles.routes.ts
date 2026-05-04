import { Routes } from '@angular/router';

export const ROLES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./role-list.component').then(m => m.RoleListComponent),
    title: 'Roles'
  },
  {
    path: 'new',
    loadComponent: () => import('./role-form.component').then(m => m.RoleFormComponent),
    title: 'New role'
  },
  {
    path: ':id',
    loadComponent: () => import('./role-detail.component').then(m => m.RoleDetailComponent),
    title: 'Role'
  }
];
