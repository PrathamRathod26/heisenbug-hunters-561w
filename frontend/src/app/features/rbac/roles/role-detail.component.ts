import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RoleService } from '../../../core/services/role.service';
import { PermissionService } from '../../../core/services/permission.service';
import { RoleResponse } from '../../../core/models/role.model';
import { PermissionResponse } from '../../../core/models/permission.model';

@Component({
  selector: 'app-role-detail',
  imports: [
    DatePipe, FormsModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatCheckboxModule, MatSlideToggleModule, MatFormFieldModule, MatInputModule
  ],
  templateUrl: './role-detail.component.html'
})
export class RoleDetailComponent implements OnInit {
  private readonly svc = inject(RoleService);
  private readonly permSvc = inject(PermissionService);
  private readonly snack = inject(MatSnackBar);

  readonly id = input.required<string>();
  role = signal<RoleResponse | null>(null);
  allPermissions = signal<PermissionResponse[]>([]);
  editDescription = '';
  editActive = true;
  selected = signal<Set<string>>(new Set());
  readonly permCols = ['capability', 'scope'];

  ngOnInit(): void {
    this.load();
    this.permSvc.list().subscribe(p => this.allPermissions.set(p));
  }

  load(): void {
    this.svc.get(this.id()).subscribe(r => {
      this.role.set(r);
      this.editDescription = r.description ?? '';
      this.editActive = r.active;
      this.selected.set(new Set(r.permissions.map(p => p.id)));
    });
  }

  saveProfile(): void {
    this.svc.update(this.id(), {
      description: this.editDescription,
      active: this.editActive
    }).subscribe(r => {
      this.role.set(r);
      this.snack.open('Role updated', 'OK', { duration: 2500 });
    });
  }

  toggle(pid: string, checked: boolean): void {
    const next = new Set(this.selected());
    checked ? next.add(pid) : next.delete(pid);
    this.selected.set(next);
  }

  isSelected(pid: string): boolean { return this.selected().has(pid); }

  savePermissions(): void {
    const ids = Array.from(this.selected());
    if (ids.length === 0) {
      this.snack.open('Select at least one permission', 'OK', { duration: 3000 });
      return;
    }
    this.svc.replacePermissions(this.id(), { permissionIds: ids }).subscribe(r => {
      this.role.set(r);
      this.snack.open('Permissions updated', 'OK', { duration: 2500 });
    });
  }
}
