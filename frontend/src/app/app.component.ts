import { Component, computed, inject } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import { AuthService, Persona } from './core/auth/auth.service';

interface NavItem {
  path: string;
  icon: string;
  label: string;
  personas: Persona[];
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly allNavItems: NavItem[] = [
    { path: '/dashboard',         icon: 'dashboard',                label: 'Dashboard',        personas: ['POLICYHOLDER', 'ADJUSTER', 'ADMIN'] },
    { path: '/claims',            icon: 'description',              label: 'Claims',           personas: ['POLICYHOLDER', 'ADJUSTER', 'ADMIN'] },
    { path: '/claims/new',        icon: 'post_add',                 label: 'File new claim',   personas: ['POLICYHOLDER'] },
    { path: '/rbac/users',        icon: 'person',                   label: 'Users',            personas: ['ADMIN'] },
    { path: '/rbac/roles',        icon: 'verified_user',            label: 'Roles',            personas: ['ADMIN'] },
    { path: '/rbac/permissions',  icon: 'key',                      label: 'Permissions',      personas: ['ADMIN'] },
    { path: '/rbac/doa-matrix',   icon: 'account_balance_wallet',   label: 'Payout limits',    personas: ['ADMIN'] }
  ];

  readonly navItems = computed<NavItem[]>(() => {
    const p = this.auth.persona();
    if (!p) return [];
    return this.allNavItems.filter(i => i.personas.includes(p));
  });

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(e => e.urlAfterRedirects),
      startWith(this.router.url)
    ),
    { initialValue: this.router.url }
  );

  readonly showShell = computed(() => {
    const u = this.currentUrl();
    return this.auth.isAuthenticated() && !u.startsWith('/login');
  });

  personaLabel(p: Persona | null): string {
    switch (p) {
      case 'POLICYHOLDER': return 'Policyholder';
      case 'ADJUSTER':     return 'Claims Adjuster';
      case 'ADMIN':        return 'Insurance Administrator';
      default:             return '—';
    }
  }
}
