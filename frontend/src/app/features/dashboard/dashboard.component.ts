import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { ClaimService } from '../../core/services/claim.service';
import { UserService } from '../../core/services/user.service';
import { RoleService } from '../../core/services/role.service';
import { PermissionService } from '../../core/services/permission.service';
import { ClaimStatus } from '../../core/models/claim.model';
import { RoleName } from '../../core/models/role.model';
import { PermissionResponse, PermissionScope } from '../../core/models/permission.model';

const CLAIM_STATUS_COLORS: Record<string, string> = {
  FNOL_RECEIVED:     '#0277bd',
  SUBMITTED:         '#29b6f6',
  TRIAGE:            '#7e57c2',
  HUMAN_REVIEW:      '#f57c00',
  SURVEYOR_REQUIRED: '#6d4c41',
  APPROVED:          '#2e7d32',
  REJECTED:          '#c62828',
  CLOSED:            '#546e7a'
};

const SCOPE_COLORS: Record<PermissionScope, string> = {
  GRANTED:               '#2e7d32',
  READ_ONLY:             '#546e7a',
  ASSIGNMENT_RESTRICTED: '#f57f17',
  FOUR_EYE_REQUIRED:     '#c62828'
};

const ROLE_PALETTE: string[] = [
  '#1565c0', '#0277bd', '#00838f', '#00695c', '#2e7d32',
  '#558b2f', '#827717', '#9e9d24', '#ef6c00', '#bf360c', '#4e342e'
];

@Component({
  selector: 'app-dashboard',
  imports: [
    DecimalPipe, RouterLink,
    MatCardModule, MatIconModule, MatProgressBarModule, MatButtonModule,
    BaseChartDirective
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private readonly claimSvc = inject(ClaimService);
  private readonly userSvc = inject(UserService);
  private readonly roleSvc = inject(RoleService);
  private readonly permSvc = inject(PermissionService);

  claimCounts = signal<Partial<Record<ClaimStatus, number>>>({});
  userCounts = signal<Partial<Record<RoleName, number>>>({});
  permissions = signal<PermissionResponse[]>([]);

  totalClaims = computed(() => Object.values(this.claimCounts()).reduce((a, b) => a + (b ?? 0), 0));
  totalUsers = computed(() => Object.values(this.userCounts()).reduce((a, b) => a + (b ?? 0), 0));
  totalRoles = signal(0);
  totalPermissions = computed(() => this.permissions().length);
  loading = signal(true);

  readonly claimChartData = computed<ChartConfiguration<'doughnut'>['data']>(() => {
    const entries = Object.entries(this.claimCounts()).filter(([, v]) => (v ?? 0) > 0);
    return {
      labels: entries.map(([k]) => k),
      datasets: [{
        data: entries.map(([, v]) => v ?? 0),
        backgroundColor: entries.map(([k]) => CLAIM_STATUS_COLORS[k] ?? '#90a4ae'),
        borderColor: '#ffffff',
        borderWidth: 2,
        hoverOffset: 8
      }]
    };
  });

  readonly claimChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: {
      legend: { position: 'right', labels: { boxWidth: 12, font: { size: 12 } } },
      tooltip: {
        callbacks: {
          label: ctx => `${ctx.label}: ${ctx.parsed} (${this.pct(ctx.parsed, this.totalClaims())})`
        }
      }
    }
  };

  readonly userChartData = computed<ChartConfiguration<'bar'>['data']>(() => {
    const entries = Object.entries(this.userCounts()).filter(([, v]) => (v ?? 0) > 0);
    return {
      labels: entries.map(([k]) => k),
      datasets: [{
        label: 'Active users',
        data: entries.map(([, v]) => v ?? 0),
        backgroundColor: entries.map((_, i) => ROLE_PALETTE[i % ROLE_PALETTE.length]),
        borderRadius: 4,
        borderSkipped: false
      }]
    };
  });

  readonly userChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: ctx => `${ctx.parsed.x} user(s)`
        }
      }
    },
    scales: {
      x: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: '#eceff1' } },
      y: { grid: { display: false }, ticks: { font: { size: 11 } } }
    }
  };

  readonly scopeChartData = computed<ChartConfiguration<'doughnut'>['data']>(() => {
    const byScope = this.permissions().reduce<Record<string, number>>((acc, p) => {
      acc[p.scope] = (acc[p.scope] ?? 0) + 1;
      return acc;
    }, {});
    const entries = Object.entries(byScope);
    return {
      labels: entries.map(([k]) => k),
      datasets: [{
        data: entries.map(([, v]) => v),
        backgroundColor: entries.map(([k]) => SCOPE_COLORS[k as PermissionScope] ?? '#90a4ae'),
        borderColor: '#ffffff',
        borderWidth: 2,
        hoverOffset: 8
      }]
    };
  });

  readonly scopeChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '62%',
    plugins: {
      legend: { position: 'right', labels: { boxWidth: 12, font: { size: 12 } } },
      tooltip: {
        callbacks: {
          label: ctx => `${ctx.label}: ${ctx.parsed} (${this.pct(ctx.parsed, this.totalPermissions())})`
        }
      }
    }
  };

  ngOnInit(): void {
    this.claimSvc.statusCounts().subscribe(counts => this.claimCounts.set(counts));
    this.userSvc.activeUsersPerRole().subscribe(counts => this.userCounts.set(counts));
    this.roleSvc.list(true).subscribe(r => this.totalRoles.set(r.length));
    this.permSvc.list().subscribe(p => {
      this.permissions.set(p);
      this.loading.set(false);
    });
  }

  private pct(v: number, total: number): string {
    return total === 0 ? '0%' : `${Math.round((v / total) * 100)}%`;
  }
}
