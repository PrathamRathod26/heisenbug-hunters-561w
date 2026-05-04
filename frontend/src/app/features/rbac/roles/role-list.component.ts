import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { RoleService } from '../../../core/services/role.service';
import { RoleResponse } from '../../../core/models/role.model';

@Component({
  selector: 'app-role-list',
  imports: [
    DatePipe, FormsModule, RouterLink,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule, MatSlideToggleModule
  ],
  templateUrl: './role-list.component.html'
})
export class RoleListComponent implements OnInit {
  private readonly svc = inject(RoleService);
  roles = signal<RoleResponse[]>([]);
  includeInactive = false;
  readonly cols = ['name', 'description', 'permissionCount', 'active', 'updatedAt', 'actions'];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.svc.list(this.includeInactive).subscribe(r => this.roles.set(r));
  }
}
