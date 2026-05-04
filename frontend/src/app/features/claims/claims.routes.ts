import { Routes } from '@angular/router';

export const CLAIMS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./claim-list.component').then(m => m.ClaimListComponent),
    title: 'Claims'
  },
  {
    path: 'new',
    loadComponent: () => import('./claim-form.component').then(m => m.ClaimFormComponent),
    title: 'New claim'
  },
  {
    path: ':id',
    loadComponent: () => import('./claim-detail.component').then(m => m.ClaimDetailComponent),
    title: 'Claim'
  }
];
