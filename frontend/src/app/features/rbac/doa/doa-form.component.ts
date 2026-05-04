import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DoaService } from '../../../core/services/doa.service';
import { RoleService } from '../../../core/services/role.service';
import { LINES_OF_BUSINESS } from '../../../core/models/doa.model';
import { RoleResponse } from '../../../core/models/role.model';

@Component({
  selector: 'app-doa-form',
  imports: [
    ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule
  ],
  templateUrl: './doa-form.component.html'
})
export class DoaFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(DoaService);
  private readonly roleSvc = inject(RoleService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly lobs = LINES_OF_BUSINESS;
  roles = signal<RoleResponse[]>([]);

  form = this.fb.nonNullable.group({
    roleId:            ['', [Validators.required]],
    lineOfBusiness:    ['MOTOR_OD' as const, [Validators.required]],
    geo:               ['IN-MH', [Validators.required, Validators.pattern(/^[A-Z]{2}(-[A-Z0-9]{1,3})?$/)]],
    approveUpToRupees: [0, [Validators.required, Validators.min(0)]],
    fourEyeAboveRupees:[0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.roleSvc.list(true).subscribe(r => this.roles.set(r));
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.svc.create({
      roleId: v.roleId,
      lineOfBusiness: v.lineOfBusiness,
      geo: v.geo,
      approveUpToPaise: (BigInt(Math.round(v.approveUpToRupees * 100))).toString(),
      fourEyeAbovePaise: (BigInt(Math.round(v.fourEyeAboveRupees * 100))).toString()
    }).subscribe(m => {
      this.snack.open('DOA entry created', 'OK', { duration: 3000 });
      this.router.navigate(['/rbac/doa-matrix', m.id]);
    });
  }
}
