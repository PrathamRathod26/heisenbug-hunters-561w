import { RoleName } from './role.model';

export type UserType = 'POLICYHOLDER' | 'INTERNAL' | 'SURVEYOR' | 'SYSTEM';
export const USER_TYPES: UserType[] = ['POLICYHOLDER', 'INTERNAL', 'SURVEYOR', 'SYSTEM'];

export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
export const USER_STATUSES: UserStatus[] = ['ACTIVE', 'SUSPENDED', 'DEACTIVATED'];

export type AssignmentStatus = 'ACTIVE' | 'REVOKED';

export interface UserCreateRequest {
  externalId: string;
  email: string;
  displayName: string;
  userType: UserType;
  licenceId?: string;
  licenceExpiresAt?: string;
}

export interface UserUpdateRequest {
  displayName?: string;
  status?: UserStatus;
  licenceId?: string;
  licenceExpiresAt?: string;
}

export interface UserSummaryResponse {
  id: string;
  email: string;
  displayName: string;
  userType: UserType;
  status: UserStatus;
}

export interface UserRoleAssignmentResponse {
  id: string;
  roleId: string;
  roleName: RoleName;
  status: AssignmentStatus;
  note?: string;
  grantedByEmail?: string;
  grantedAt: string;
  revokedAt?: string;
  revokedByEmail?: string;
}

export interface UserResponse {
  id: string;
  externalId: string;
  email: string;
  displayName: string;
  userType: UserType;
  status: UserStatus;
  licenceId?: string;
  licenceExpiresAt?: string;
  roleAssignments: UserRoleAssignmentResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface UserRoleAssignRequest {
  roleId: string;
  note?: string;
  grantedByUserId?: string;
}

export interface EffectivePermissionsResponse {
  userId: string;
  activeRoles: RoleName[];
  permissions: CapabilityPermission[];
}

export interface CapabilityPermission {
  capability: string;
  effectiveScope: 'GRANTED' | 'READ_ONLY' | 'ASSIGNMENT_RESTRICTED' | 'FOUR_EYE_REQUIRED';
  grantedBy: RoleName[];
}
