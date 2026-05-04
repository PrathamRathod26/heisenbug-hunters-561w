import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AuthService } from '../../core/auth/auth.service';
import { UserSummaryResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-login',
  imports: [MatCardModule, MatButtonModule, MatIconModule, MatListModule, MatProgressBarModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snack = inject(MatSnackBar);

  candidates = signal<UserSummaryResponse[]>([]);
  loading = signal(true);
  submitting = signal<string | null>(null);

  ngOnInit(): void {
    if (this.auth.isAuthenticated()) {
      this.router.navigate([this.redirectTarget()]);
      return;
    }
    this.auth.listLoginCandidates().subscribe({
      next: list => {
        this.candidates.set(list);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  signIn(u: UserSummaryResponse): void {
    this.submitting.set(u.id);
    this.auth.login(u.id).subscribe({
      next: user => {
        this.submitting.set(null);
        if (!user) {
          this.snack.open(
            `${u.email} does not have a Policyholder / Claims Adjuster / Administrator role and cannot sign in here.`,
            'Dismiss',
            { duration: 6000 }
          );
          return;
        }
        this.snack.open(`Signed in as ${user.email}`, 'OK', { duration: 2500 });
        this.router.navigateByUrl(this.redirectTarget());
      },
      error: () => this.submitting.set(null)
    });
  }

  redirectTarget(): string {
    const qp = this.route.snapshot.queryParamMap.get('redirectTo');
    return qp && qp !== '/login' ? qp : '/dashboard';
  }

  typePillClass(t: string): string {
    return t === 'INTERNAL' ? 'pill pill-ok'
         : t === 'POLICYHOLDER' ? 'pill pill-muted'
         : t === 'SURVEYOR' ? 'pill pill-warn'
         : 'pill pill-err';
  }
}
