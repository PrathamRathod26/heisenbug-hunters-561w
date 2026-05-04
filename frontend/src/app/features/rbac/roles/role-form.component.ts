import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RoleService } from '../../../core/services/role.service';
import { PermissionService } from '../../../core/services/permission.service';
import { ROLE_NAMES } from '../../../core/models/role.model';
import { PermissionResponse } from '../../../core/models/permission.model';

@Component({
  selector: 'app-role-form',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatCheckboxModule
  ],
  templateUrl: './role-form.component.html'
})
export class RoleFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(RoleService);
  private readonly permSvc = inject(PermissionService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly roleNames = ROLE_NAMES;
  permissions = signal<PermissionResponse[]>([]);
  selected = signal<Set<string>>(new Set());

  form = this.fb.nonNullable.group({
    name: ['ADJUSTER' as const, [Validators.required]],
    description: ['', [Validators.maxLength(255)]]
  });

  ngOnInit(): void {
    this.permSvc.list().subscribe(p => this.permissions.set(p));
  }

  toggle(id: string, checked: boolean): void {
    const next = new Set(this.selected());
    checked ? next.add(id) : next.delete(id);
    this.selected.set(next);
  }

  isSelected(id: string): boolean { return this.selected().has(id); }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.svc.create({
      name: v.name,
      description: v.description || undefined,
      permissionIds: Array.from(this.selected())
    }).subscribe(r => {
      this.snack.open(`Role ${r.name} created`, 'OK', { duration: 3000 });
      this.router.navigate(['/rbac/roles', r.id]);
    });
  }
}
