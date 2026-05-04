import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DecimalPipe } from '@angular/common';
import { ClaimService } from '../../core/services/claim.service';
import { PaiseInrPipe } from '../../shared/paise.pipe';
import { AnalyzeResponse, ClaimWithAnalysisResponse } from '../../core/models/analyze.model';

interface ImageSlot {
  file: File;
  preview: string;
  sizeKb: number;
}

@Component({
  selector: 'app-claim-form',
  imports: [
    DecimalPipe, ReactiveFormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressBarModule,
    PaiseInrPipe
  ],
  templateUrl: './claim-form.component.html',
  styleUrl: './claim-form.component.scss'
})
export class ClaimFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly svc = inject(ClaimService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly maxImages = signal(10);
  readonly minImages = signal(3);
  readonly images = signal<ImageSlot[]>([]);
  readonly video = signal<File | null>(null);
  readonly submitting = signal(false);
  readonly result = signal<ClaimWithAnalysisResponse | null>(null);

  form = this.fb.nonNullable.group({
    policyNumber: ['', [Validators.required, Validators.pattern(/^[A-Z0-9-]{6,32}$/)]],
    claimantName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    estimatedLoss: [0, [Validators.required, Validators.min(0)]],
    narrative: ['', [Validators.maxLength(4000)]],
    incidentTime: ['']
  });

  ngOnInit(): void {
    this.svc.config().subscribe({
      next: cfg => {
        this.maxImages.set(cfg.maxEvidences);
        this.minImages.set(cfg.minEvidencesForAnalysis);
      },
      error: () => {}
    });
  }

  onFilesSelected(evt: Event): void {
    const input = evt.target as HTMLInputElement;
    const picked = Array.from(input.files ?? []);
    input.value = '';
    if (picked.length === 0) return;

    const current = this.images();
    const room = this.maxImages() - current.length;
    if (room <= 0) {
      this.snack.open(`Already at maximum of ${this.maxImages()} images.`, 'OK', { duration: 3000 });
      return;
    }
    const accepted = picked.filter(f => f.type.startsWith('image/')).slice(0, room);
    const rejectedNonImage = picked.length - picked.filter(f => f.type.startsWith('image/')).length;
    const truncated = picked.filter(f => f.type.startsWith('image/')).length > room;

    const added: ImageSlot[] = accepted.map(file => ({
      file,
      preview: URL.createObjectURL(file),
      sizeKb: Math.round(file.size / 1024)
    }));
    this.images.set([...current, ...added]);

    if (rejectedNonImage > 0) {
      this.snack.open(`${rejectedNonImage} non-image file(s) skipped.`, 'OK', { duration: 3000 });
    }
    if (truncated) {
      this.snack.open(`Capped at ${this.maxImages()} images.`, 'OK', { duration: 3000 });
    }
  }

  removeImage(i: number): void {
    const next = this.images().slice();
    const [removed] = next.splice(i, 1);
    if (removed) URL.revokeObjectURL(removed.preview);
    this.images.set(next);
  }

  onVideoSelected(evt: Event): void {
    const input = evt.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!file.type.startsWith('video/')) {
      this.snack.open('Please select a video file.', 'OK', { duration: 3000 });
      return;
    }
    this.video.set(file);
  }

  removeVideo(): void {
    this.video.set(null);
  }

  videoSizeMb(): string {
    const f = this.video();
    return f ? (f.size / (1024 * 1024)).toFixed(1) : '0';
  }

  canSubmit(): boolean {
    return this.form.valid
        && this.images().length >= this.minImages()
        && this.images().length <= this.maxImages()
        && !this.submitting();
  }

  submit(): void {
    if (!this.canSubmit()) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.submitting.set(true);
    this.svc.createWithAnalysis({
      policyNumber: v.policyNumber,
      claimantName: v.claimantName,
      estimatedLoss: v.estimatedLoss,
      narrative: v.narrative || undefined,
      incidentTime: v.incidentTime || undefined,
      images: this.images().map(s => s.file),
      video: this.video() ?? undefined
    }).subscribe({
      next: res => {
        this.submitting.set(false);
        this.result.set(res);
        this.snack.open(
          `Claim ${res.claim.policyNumber} created — ${res.analysis.totalDetections} detection(s) from ML`,
          'OK',
          { duration: 4000 }
        );
      },
      error: () => this.submitting.set(false)
    });
  }

  severityPill(v: string | undefined): string {
    switch ((v ?? '').toLowerCase()) {
      case 'minor':                return 'pill pill-ok';
      case 'moderate':             return 'pill pill-warn';
      case 'severe':               return 'pill pill-err';
      case 'total_loss_candidate': return 'pill pill-err';
      default:                     return 'pill pill-muted';
    }
  }

  goToClaim(): void {
    const id = this.result()?.claim.id;
    if (id) this.router.navigate(['/claims', id]);
  }

  resetForAnother(): void {
    this.images().forEach(s => URL.revokeObjectURL(s.preview));
    this.images.set([]);
    this.video.set(null);
    this.form.reset({
      policyNumber: '',
      claimantName: '',
      estimatedLoss: 0,
      narrative: '',
      incidentTime: ''
    });
    this.result.set(null);
  }

  formatConfidence(c: number | undefined): string {
    if (c == null) return '—';
    return `${Math.round(c * 100)}%`;
  }
}
