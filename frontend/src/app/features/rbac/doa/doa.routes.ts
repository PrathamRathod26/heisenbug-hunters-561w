import { Routes } from '@angular/router';

export const DOA_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./doa-list.component').then(m => m.DoaListComponent),
    title: 'DOA Matrix'
  },
  {
    path: 'new',
    loadComponent: () => import('./doa-form.component').then(m => m.DoaFormComponent),
    title: 'New DOA entry'
  },
  {
    path: ':id',
    loadComponent: () => import('./doa-detail.component').then(m => m.DoaDetailComponent),
    title: 'DOA entry'
  }
];
