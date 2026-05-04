import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { UserService } from '../../../core/services/user.service';
import { USER_TYPES } from '../../../core/models/user.model';

@Component({
  selector: 'app-user-form',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule
  ],
  templateUrl: './user-form.component.html'
})
export class UserFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(UserService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly userTypes = USER_TYPES;

  form = this.fb.nonNullable.group({
    externalId:       ['', [Validators.required, Validators.maxLength(128)]],
    email:            ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    displayName:      ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    userType:         ['INTERNAL' as const, [Validators.required]],
    licenceId:        [''],
    licenceExpiresAt: ['']
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const payload = {
      externalId: v.externalId,
      email: v.email,
      displayName: v.displayName,
      userType: v.userType,
      ...(v.licenceId ? { licenceId: v.licenceId } : {}),
      ...(v.licenceExpiresAt ? { licenceExpiresAt: new Date(v.licenceExpiresAt).toISOString() } : {})
    };
    this.svc.create(payload).subscribe(u => {
      this.snack.open(`User ${u.email} created`, 'OK', { duration: 3000 });
      this.router.navigate(['/rbac/users', u.id]);
    });
  }
}
