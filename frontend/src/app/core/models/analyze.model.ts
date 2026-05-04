import { ClaimResponse } from './claim.model';

export interface AnalyzeDetection {
  label: string;
  confidence: number;
  severity: string;
  severityScore: number;
}

export interface AnalyzeImageResult {
  imageId: string;
  source: string;
  sourceTimestampSec: number | null;
  width: number;
  height: number;
  detections: AnalyzeDetection[];
  latencyMs: number;
}

export interface AnalyzeCostEstimate {
  currency: string;
  subtotalPaise: number;
  taxPaise: number;
  discountPaise: number;
  totalPaise: number;
  totalLowPaise: number;
  totalHighPaise: number;
  gstRate: number;
  catalogVersion: string;
  assumptions: string[];
  requiresHumanReview: boolean;
  reviewReasons: string[];
}

export interface SurveyorAssessment {
  summary: string;
  severityVerdict: string;
  repairRecommendation: string;
  assessmentConfidence: number;
  modelUsed: string;
  systemPromptVersion: string;
  generatedAt: string;
}

export interface AnalyzeResponse {
  modelVersion: string;
  images: AnalyzeImageResult[];
  totalDetections: number;
  costEstimate: AnalyzeCostEstimate | null;
  surveyorAssessment: SurveyorAssessment | null;
  processingTimeMs: number;
}

export interface ClaimWithAnalysisResponse {
  claim: ClaimResponse;
  analysis: AnalyzeResponse;
}

export interface ClaimsConfig {
  maxEvidences: number;
  minEvidencesForAnalysis: number;
}
