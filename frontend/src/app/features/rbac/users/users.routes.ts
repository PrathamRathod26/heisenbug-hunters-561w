import { Routes } from '@angular/router';

export const USERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./user-list.component').then(m => m.UserListComponent),
    title: 'Users'
  },
  {
    path: 'new',
    loadComponent: () => import('./user-form.component').then(m => m.UserFormComponent),
    title: 'New user'
  },
  {
    path: ':id',
    loadComponent: () => import('./user-detail.component').then(m => m.UserDetailComponent),
    title: 'User'
  }
];
