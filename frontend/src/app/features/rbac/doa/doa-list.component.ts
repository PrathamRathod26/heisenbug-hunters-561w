import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { DoaService } from '../../../core/services/doa.service';
import { RoleService } from '../../../core/services/role.service';
import { DoaMatrixResponse } from '../../../core/models/doa.model';
import { RoleResponse } from '../../../core/models/role.model';
import { PaiseInrPipe } from '../../../shared/paise.pipe';

@Component({
  selector: 'app-doa-list',
  imports: [
    DatePipe, FormsModule, RouterLink,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatSelectModule, PaiseInrPipe
  ],
  templateUrl: './doa-list.component.html'
})
export class DoaListComponent implements OnInit {
  private readonly svc = inject(DoaService);
  private readonly roleSvc = inject(RoleService);

  rows = signal<DoaMatrixResponse[]>([]);
  roles = signal<RoleResponse[]>([]);
  filterRoleId = '';
  readonly cols = ['roleName', 'lineOfBusiness', 'geo', 'approveUpToPaise', 'fourEyeAbovePaise', 'active', 'updatedAt', 'actions'];

  ngOnInit(): void {
    this.load();
    this.roleSvc.list(true).subscribe(r => this.roles.set(r));
  }

  load(): void {
    this.svc.list(this.filterRoleId || undefined).subscribe(r => this.rows.set(r));
  }
}
