import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { ClaimService } from '../../core/services/claim.service';
import { CLAIM_STATUSES, ClaimStatus, ClaimSummaryResponse } from '../../core/models/claim.model';
import { Page } from '../../core/models/common.model';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-claim-list',
  imports: [
    CurrencyPipe, DatePipe, FormsModule, RouterLink,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatPaginatorModule, MatFormFieldModule, MatSelectModule
  ],
  templateUrl: './claim-list.component.html'
})
export class ClaimListComponent implements OnInit {
  private readonly svc = inject(ClaimService);
  readonly auth = inject(AuthService);

  status: ClaimStatus = 'FNOL_RECEIVED';
  readonly statuses = CLAIM_STATUSES;
  readonly displayedColumns = ['policyNumber', 'claimantName', 'status', 'estimatedLoss', 'createdAt', 'actions'];

  page = signal<Page<ClaimSummaryResponse> | null>(null);
  pageIndex = 0;
  pageSize = 25;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.svc.list(this.status, this.pageIndex, this.pageSize)
      .subscribe(p => this.page.set(p));
  }

  onStatusChange(): void { this.pageIndex = 0; this.load(); }

  onPage(e: PageEvent): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.load();
  }
}
