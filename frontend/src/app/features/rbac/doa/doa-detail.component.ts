import { Component, OnInit, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DoaService } from '../../../core/services/doa.service';
import { DoaMatrixResponse } from '../../../core/models/doa.model';
import { PaiseInrPipe } from '../../../shared/paise.pipe';

@Component({
  selector: 'app-doa-detail',
  imports: [
    DatePipe, FormsModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSlideToggleModule, PaiseInrPipe
  ],
  templateUrl: './doa-detail.component.html'
})
export class DoaDetailComponent implements OnInit {
  private readonly svc = inject(DoaService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly id = input.required<string>();
  entry = signal<DoaMatrixResponse | null>(null);
  editApproveUpToRupees = 0;
  editFourEyeAboveRupees = 0;
  editActive = true;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.svc.get(this.id()).subscribe(m => {
      this.entry.set(m);
      this.editApproveUpToRupees = Number(BigInt(m.approveUpToPaise) / 100n);
      this.editFourEyeAboveRupees = Number(BigInt(m.fourEyeAbovePaise) / 100n);
      this.editActive = m.active;
    });
  }

  save(): void {
    this.svc.update(this.id(), {
      approveUpToPaise: (BigInt(Math.round(this.editApproveUpToRupees * 100))).toString(),
      fourEyeAbovePaise: (BigInt(Math.round(this.editFourEyeAboveRupees * 100))).toString(),
      active: this.editActive
    }).subscribe(m => {
      this.entry.set(m);
      this.snack.open('DOA entry updated', 'OK', { duration: 2500 });
    });
  }

  remove(): void {
    if (!confirm('Delete this DOA entry?')) return;
    this.svc.delete(this.id()).subscribe(() => {
      this.snack.open('Deleted', 'OK', { duration: 2500 });
      this.router.navigate(['/rbac/doa-matrix']);
    });
  }
}
