import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { UserService } from '../../../core/services/user.service';
import { RoleService } from '../../../core/services/role.service';
import { EffectivePermissionsResponse, UserResponse, UserStatus } from '../../../core/models/user.model';
import { USER_STATUSES } from '../../../core/models/user.model';
import { RoleResponse } from '../../../core/models/role.model';

@Component({
  selector: 'app-user-detail',
  imports: [
    DatePipe, FormsModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatChipsModule, MatExpansionModule
  ],
  templateUrl: './user-detail.component.html'
})
export class UserDetailComponent implements OnInit {
  private readonly svc = inject(UserService);
  private readonly roleSvc = inject(RoleService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly id = input.required<string>();

  user = signal<UserResponse | null>(null);
  effective = signal<EffectivePermissionsResponse | null>(null);
  availableRoles = signal<RoleResponse[]>([]);

  editStatus: UserStatus | null = null;
  editDisplayName = '';
  newRoleId = '';
  newRoleNote = '';

  readonly statuses = USER_STATUSES;
  readonly assignmentCols = ['roleName', 'status', 'grantedAt', 'grantedByEmail', 'note', 'actions'];

  ngOnInit(): void {
    this.load();
    this.roleSvc.list().subscribe(r => this.availableRoles.set(r));
  }

  load(): void {
    this.svc.get(this.id()).subscribe(u => {
      this.user.set(u);
      this.editStatus = u.status;
      this.editDisplayName = u.displayName;
    });
    this.svc.effectivePermissions(this.id()).subscribe(e => this.effective.set(e));
  }

  saveProfile(): void {
    this.svc.update(this.id(), {
      displayName: this.editDisplayName,
      status: this.editStatus ?? undefined
    }).subscribe(u => {
      this.user.set(u);
      this.snack.open('User updated', 'OK', { duration: 2500 });
    });
  }

  assignRole(): void {
    if (!this.newRoleId) return;
    this.svc.assignRole(this.id(), {
      roleId: this.newRoleId,
      note: this.newRoleNote || undefined
    }).subscribe(() => {
      this.newRoleId = '';
      this.newRoleNote = '';
      this.load();
      this.snack.open('Role assigned', 'OK', { duration: 2500 });
    });
  }

  revoke(assignmentId: string): void {
    if (!confirm('Revoke this role?')) return;
    this.svc.revokeRole(assignmentId).subscribe(() => {
      this.load();
      this.snack.open('Role revoked', 'OK', { duration: 2500 });
    });
  }

  scopePill(scope: string): string {
    return scope === 'GRANTED' ? 'pill pill-ok'
         : scope === 'READ_ONLY' ? 'pill pill-muted'
         : scope === 'ASSIGNMENT_RESTRICTED' ? 'pill pill-warn'
         : 'pill pill-err';
  }
}
