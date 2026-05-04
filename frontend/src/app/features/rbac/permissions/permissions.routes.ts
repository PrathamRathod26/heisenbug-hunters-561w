import { Routes } from '@angular/router';

export const PERMISSIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./permission-list.component').then(m => m.PermissionListComponent),
    title: 'Permissions'
  }
];
