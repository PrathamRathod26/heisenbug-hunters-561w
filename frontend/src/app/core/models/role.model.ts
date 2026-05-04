import { PermissionResponse } from './permission.model';

export type RoleName =
  | 'POLICYHOLDER'
  | 'ADJUSTER'
  | 'SR_ADJUSTER'
  | 'FRAUD_INVESTIGATOR'
  | 'SURVEYOR'
  | 'GRIEVANCE_OFFICER'
  | 'AML_OFFICER'
  | 'FINANCE_APPROVER'
  | 'ADMIN'
  | 'AUDITOR'
  | 'SYSTEM';

export const ROLE_NAMES: RoleName[] = [
  'POLICYHOLDER', 'ADJUSTER', 'SR_ADJUSTER', 'FRAUD_INVESTIGATOR',
  'SURVEYOR', 'GRIEVANCE_OFFICER', 'AML_OFFICER', 'FINANCE_APPROVER',
  'ADMIN', 'AUDITOR', 'SYSTEM'
];

export interface RoleCreateRequest {
  name: RoleName;
  description?: string;
  permissionIds?: string[];
}

export interface RoleUpdateRequest {
  description?: string;
  active?: boolean;
}

export interface RolePermissionUpdateRequest {
  permissionIds: string[];
}

export interface RoleResponse {
  id: string;
  name: RoleName;
  description?: string;
  active: boolean;
  permissions: PermissionResponse[];
  createdAt: string;
  updatedAt: string;
}
