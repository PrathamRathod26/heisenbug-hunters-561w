import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Page } from '../models/common.model';
import {
  ClaimCreateRequest,
  ClaimResponse,
  ClaimStatus,
  ClaimSummaryResponse
} from '../models/claim.model';
import { AnalyzeResponse, ClaimWithAnalysisResponse, ClaimsConfig } from '../models/analyze.model';
import { catchError, map, of } from 'rxjs';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

export interface CreateWithAnalysisInput {
  policyNumber: string;
  claimantName: string;
  estimatedLoss: number;
  narrative?: string;
  incidentTime?: string;
  images: File[];
  video?: File | null;
}

@Injectable({ providedIn: 'root' })
export class ClaimService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBaseUrl}/api/v1/claims`;

  create(body: ClaimCreateRequest): Observable<ClaimResponse> {
    return this.http.post<ClaimResponse>(this.base, body);
  }

  createWithAnalysis(input: CreateWithAnalysisInput): Observable<ClaimWithAnalysisResponse> {
    const form = new FormData();
    form.append('policyNumber', input.policyNumber);
    form.append('claimantName', input.claimantName);
    form.append('estimatedLoss', String(input.estimatedLoss));
    if (input.narrative)    form.append('narrative', input.narrative);
    if (input.incidentTime) form.append('incidentTime', input.incidentTime);
    input.images.forEach(f => form.append('images', f, f.name));
    if (input.video) form.append('video', input.video, input.video.name);
    return this.http.post<ClaimWithAnalysisResponse>(`${this.base}/with-analysis`, form);
  }

  config(): Observable<ClaimsConfig> {
    return this.http.get<ClaimsConfig>(`${this.base}/config`);
  }

  get(id: string): Observable<ClaimResponse> {
    return this.http.get<ClaimResponse>(`${this.base}/${id}`);
  }

  getAnalysis(id: string): Observable<AnalyzeResponse | null> {
    return this.http
      .get<AnalyzeResponse>(`${this.base}/${id}/analysis`, { observe: 'response' })
      .pipe(
        map((r: HttpResponse<AnalyzeResponse>) =>
          r.status === 204 ? null : (r.body ?? null)
        ),
        catchError((err: HttpErrorResponse) =>
          err.status === 404 || err.status === 204 ? of(null) : of(null)
        )
      );
  }

  list(status: ClaimStatus, page = 0, size = 25, sort = 'createdAt,desc'): Observable<Page<ClaimSummaryResponse>> {
    const params = new HttpParams()
      .set('status', status)
      .set('page', page)
      .set('size', size)
      .set('sort', sort);
    return this.http.get<Page<ClaimSummaryResponse>>(this.base, { params });
  }

  updateStatus(id: string, newStatus: ClaimStatus): Observable<ClaimResponse> {
    const params = new HttpParams().set('newStatus', newStatus);
    return this.http.patch<ClaimResponse>(`${this.base}/${id}/status`, null, { params });
  }

  statusCounts(): Observable<Record<ClaimStatus, number>> {
    return this.http.get<Record<ClaimStatus, number>>(`${this.base}/stats/status-counts`);
  }
}
