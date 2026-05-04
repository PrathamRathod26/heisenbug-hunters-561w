import { Component, HostListener, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ImageViewerData {
  src: string;
  title?: string;
  contentType?: string;
}

@Component({
  selector: 'app-image-viewer',
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="viewer-toolbar">
      <span class="title" title="{{ data.title }}">{{ data.title || 'Evidence' }}</span>
      <span class="spacer"></span>

      <button mat-icon-button (click)="zoomOut()" [disabled]="zoom() <= MIN_ZOOM" aria-label="Zoom out">
        <mat-icon>zoom_out</mat-icon>
      </button>
      <span class="zoom-label">{{ zoomPercent() }}%</span>
      <button mat-icon-button (click)="zoomIn()" [disabled]="zoom() >= MAX_ZOOM" aria-label="Zoom in">
        <mat-icon>zoom_in</mat-icon>
      </button>
      <button mat-icon-button (click)="reset()" aria-label="Reset zoom">
        <mat-icon>restart_alt</mat-icon>
      </button>

      <a mat-icon-button [href]="data.src" target="_blank" rel="noopener" aria-label="Open in new tab">
        <mat-icon>open_in_new</mat-icon>
      </a>
      <button mat-icon-button mat-dialog-close aria-label="Close">
        <mat-icon>close</mat-icon>
      </button>
    </div>

    <div class="viewer"
         (wheel)="onWheel($event)">
      @if (isVideo()) {
        <video [src]="data.src" controls autoplay style="max-width: 100%; max-height: 80vh;"></video>
      } @else {
        <img [src]="data.src"
             [style.transform]="'scale(' + zoom() + ')'"
             alt="{{ data.title || 'evidence' }}">
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .viewer-toolbar {
      display: flex; align-items: center; gap: 4px;
      padding: 8px 12px; border-bottom: 1px solid #e0e0e0;
      background: #fafafa;
    }
    .viewer-toolbar .title {
      font-weight: 500; max-width: 320px;
      overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
    }
    .viewer-toolbar .spacer { flex: 1; }
    .zoom-label {
      font-variant-numeric: tabular-nums;
      min-width: 52px; text-align: center;
      font-size: 12px; color: #555;
    }
    .viewer {
      overflow: auto;
      max-width: 90vw;
      max-height: 82vh;
      min-width: 480px;
      min-height: 320px;
      padding: 12px;
      background: #222;
      text-align: center;
    }
    .viewer img {
      display: inline-block;
      transform-origin: center top;
      transition: transform 120ms ease;
      max-width: none;
    }
  `]
})
export class ImageViewerDialogComponent {
  readonly data = inject<ImageViewerData>(MAT_DIALOG_DATA);
  readonly MIN_ZOOM = 0.25;
  readonly MAX_ZOOM = 5;
  readonly ZOOM_STEP = 0.25;

  zoom = signal(1);

  zoomIn(): void {
    this.zoom.update(z => Math.min(this.MAX_ZOOM, +(z + this.ZOOM_STEP).toFixed(2)));
  }
  zoomOut(): void {
    this.zoom.update(z => Math.max(this.MIN_ZOOM, +(z - this.ZOOM_STEP).toFixed(2)));
  }
  reset(): void { this.zoom.set(1); }
  zoomPercent(): number { return Math.round(this.zoom() * 100); }

  isVideo(): boolean {
    return (this.data.contentType || '').startsWith('video/');
  }

  onWheel(e: WheelEvent): void {
    if (!e.ctrlKey) return;
    e.preventDefault();
    if (e.deltaY < 0) this.zoomIn();
    else this.zoomOut();
  }

  @HostListener('document:keydown', ['$event'])
  onKeyDown(e: KeyboardEvent): void {
    if (e.key === '+' || e.key === '=') this.zoomIn();
    else if (e.key === '-' || e.key === '_') this.zoomOut();
    else if (e.key === '0') this.reset();
  }
}
