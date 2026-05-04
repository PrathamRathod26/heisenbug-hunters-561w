import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-forbidden',
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="forbidden-page">
      <mat-card>
        <mat-card-header>
          <mat-card-title>
            <mat-icon color="warn">block</mat-icon>
            Not authorised
          </mat-card-title>
          <mat-card-subtitle>
            Your role does not grant access to this page.
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (auth.user(); as u) {
            <p class="muted">
              Signed in as <strong>{{ u.email }}</strong> — active roles:
              @for (r of auth.activeRoles(); track r) {
                <span class="pill pill-muted" style="margin-left: 4px;">{{ r }}</span>
              }
            </p>
          }
        </mat-card-content>
        <mat-card-actions>
          <button mat-stroked-button (click)="auth.logout()">
            <mat-icon>logout</mat-icon> Sign out
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .forbidden-page {
      min-height: 80vh;
      display: flex; align-items: center; justify-content: center;
      padding: 24px;
    }
    mat-card { max-width: 520px; }
    mat-card-title { display: flex; align-items: center; gap: 10px; }
  `]
})
export class ForbiddenComponent {
  readonly auth = inject(AuthService);
}
