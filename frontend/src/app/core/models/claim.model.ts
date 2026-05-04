export type ClaimStatus =
  | 'FNOL_RECEIVED'
  | 'SUBMITTED'
  | 'TRIAGE'
  | 'HUMAN_REVIEW'
  | 'SURVEYOR_REQUIRED'
  | 'APPROVED'
  | 'REJECTED'
  | 'CLOSED';

export const CLAIM_STATUSES: ClaimStatus[] = [
  'FNOL_RECEIVED', 'SUBMITTED', 'TRIAGE', 'HUMAN_REVIEW',
  'SURVEYOR_REQUIRED', 'APPROVED', 'REJECTED', 'CLOSED'
];

export type EvidenceType = 'PHOTO' | 'VIDEO' | 'DOCUMENT';
export const EVIDENCE_TYPES: EvidenceType[] = ['PHOTO', 'VIDEO', 'DOCUMENT'];

export interface ClaimEvidenceCreate {
  type: EvidenceType;
  uri: string;
}

export interface ClaimCreateRequest {
  policyNumber: string;
  claimantName: string;
  estimatedLoss: number;
  evidences?: ClaimEvidenceCreate[];
}

export interface ClaimEvidenceResponse {
  id: string;
  type: EvidenceType;
  uri: string;
  uploadedAt: string;
}

export interface ClaimResponse {
  id: string;
  policyNumber: string;
  claimantName: string;
  status: ClaimStatus;
  estimatedLoss: number;
  evidences: ClaimEvidenceResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface ClaimSummaryResponse {
  id: string;
  policyNumber: string;
  claimantName: string;
  status: ClaimStatus;
  estimatedLoss: number;
  createdAt: string;
}
