import { Component, OnInit, inject, input, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClaimService } from '../../core/services/claim.service';
import { CLAIM_STATUSES, ClaimEvidenceResponse, ClaimResponse, ClaimStatus } from '../../core/models/claim.model';
import { AnalyzeResponse } from '../../core/models/analyze.model';
import { PaiseInrPipe } from '../../shared/paise.pipe';
import { environment } from '../../../environments/environment';
import { ImageViewerDialogComponent } from './image-viewer-dialog.component';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-claim-detail',
  imports: [
    CurrencyPipe, DatePipe, DecimalPipe, FormsModule, RouterLink,
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatFormFieldModule, MatSelectModule, MatProgressBarModule,
    MatDialogModule, MatTooltipModule,
    PaiseInrPipe
  ],
  templateUrl: './claim-detail.component.html',
  styleUrl: './claim-detail.component.scss'
})
export class ClaimDetailComponent implements OnInit {
  private readonly svc = inject(ClaimService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly auth = inject(AuthService);

  readonly canChangeStatus = () => this.auth.persona() !== 'POLICYHOLDER';

  readonly id = input.required<string>();
  claim = signal<ClaimResponse | null>(null);
  analysis = signal<AnalyzeResponse | null>(null);
  analysisLoading = signal(true);
  newStatus: ClaimStatus | null = null;
  readonly statuses = CLAIM_STATUSES;
  readonly evidenceColumns = ['preview', 'type', 'uri', 'uploadedAt'];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.svc.get(this.id()).subscribe(c => {
      this.claim.set(c);
      this.newStatus = c.status;
    });
    this.analysisLoading.set(true);
    this.svc.getAnalysis(this.id()).subscribe({
      next: a => {
        this.analysis.set(a);
        this.analysisLoading.set(false);
      },
      error: () => this.analysisLoading.set(false)
    });
  }

  updateStatus(): void {
    if (!this.newStatus) return;
    this.svc.updateStatus(this.id(), this.newStatus).subscribe(c => {
      this.claim.set(c);
      this.snack.open(`Status updated to ${c.status}`, 'OK', { duration: 2500 });
    });
  }

  back(): void { this.router.navigate(['/claims']); }

  severityPill(v: string | undefined | null): string {
    switch ((v ?? '').toLowerCase()) {
      case 'minor':                return 'pill pill-ok';
      case 'moderate':             return 'pill pill-warn';
      case 'severe':               return 'pill pill-err';
      case 'total_loss_candidate': return 'pill pill-err';
      default:                     return 'pill pill-muted';
    }
  }

  formatConfidence(c: number | undefined | null): string {
    if (c == null) return '—';
    return `${Math.round(c * 100)}%`;
  }

  detectionCount(a: AnalyzeResponse | null): number {
    if (!a) return 0;
    if (a.totalDetections != null) return a.totalDetections;
    return (a.images ?? []).reduce((n, img) => n + (img.detections?.length ?? 0), 0);
  }

  isAnalysisUnavailable(a: AnalyzeResponse | null): boolean {
    return !!a && (a.modelVersion === 'ml-service-unavailable' || this.detectionCount(a) === 0);
  }

  evidenceUrl(ev: ClaimEvidenceResponse): string {
    return `${environment.apiBaseUrl}/api/v1/claims/${this.id()}/evidence/${ev.id}`;
  }

  isImage(ev: ClaimEvidenceResponse): boolean {
    return ev.type === 'PHOTO';
  }

  isVideo(ev: ClaimEvidenceResponse): boolean {
    return ev.type === 'VIDEO';
  }

  openEvidence(ev: ClaimEvidenceResponse): void {
    if (!this.isImage(ev) && !this.isVideo(ev)) {
      window.open(this.evidenceUrl(ev), '_blank', 'noopener');
      return;
    }
    this.dialog.open(ImageViewerDialogComponent, {
      data: {
        src: this.evidenceUrl(ev),
        title: ev.uri,
        contentType: this.isVideo(ev) ? 'video/*' : 'image/*'
      },
      maxWidth: '95vw',
      maxHeight: '95vh',
      panelClass: 'image-viewer-dialog'
    });
  }

  onThumbError(evt: Event): void {
    const img = evt.target as HTMLImageElement;
    img.style.display = 'none';
    const parent = img.parentElement;
    if (parent) parent.classList.add('thumb-missing');
  }
}
