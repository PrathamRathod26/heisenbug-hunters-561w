import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginator, MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { UserService } from '../../../core/services/user.service';
import { USER_STATUSES, USER_TYPES, UserStatus, UserSummaryResponse, UserType } from '../../../core/models/user.model';
import { Page } from '../../../core/models/common.model';

@Component({
  selector: 'app-user-list',
  imports: [
    FormsModule, RouterLink,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatPaginatorModule, MatFormFieldModule, MatSelectModule
  ],
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit {
  private readonly svc = inject(UserService);

  type: UserType | '' = '';
  status: UserStatus | '' = '';
  readonly userTypes = USER_TYPES;
  readonly userStatuses = USER_STATUSES;
  readonly cols = ['email', 'displayName', 'userType', 'status', 'actions'];

  page = signal<Page<UserSummaryResponse> | null>(null);
  pageIndex = 0;
  pageSize = 25;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.svc.list({
      type: this.type || undefined,
      status: this.status || undefined,
      page: this.pageIndex,
      size: this.pageSize
    }).subscribe(p => this.page.set(p));
  }

  filterChanged(): void { this.pageIndex = 0; this.load(); }

  onPage(e: PageEvent): void {
    this.pageIndex = e.pageIndex;
    this.pageSize = e.pageSize;
    this.load();
  }

  statusPillClass(s: UserStatus): string {
    return s === 'ACTIVE' ? 'pill pill-ok' : s === 'SUSPENDED' ? 'pill pill-warn' : 'pill pill-err';
  }
}
