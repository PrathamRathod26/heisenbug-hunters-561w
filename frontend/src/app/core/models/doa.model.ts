import { RoleName } from './role.model';

export type LineOfBusiness =
  | 'MOTOR_OD'
  | 'MOTOR_TP'
  | 'HEALTH'
  | 'PROPERTY'
  | 'MARINE'
  | 'ENGINEERING';

export const LINES_OF_BUSINESS: LineOfBusiness[] = [
  'MOTOR_OD', 'MOTOR_TP', 'HEALTH', 'PROPERTY', 'MARINE', 'ENGINEERING'
];

export interface DoaMatrixCreateRequest {
  roleId: string;
  lineOfBusiness: LineOfBusiness;
  geo: string;
  approveUpToPaise: string;
  fourEyeAbovePaise: string;
}

export interface DoaMatrixUpdateRequest {
  approveUpToPaise?: string;
  fourEyeAbovePaise?: string;
  active?: boolean;
}

export interface DoaMatrixResponse {
  id: string;
  roleId: string;
  roleName: RoleName;
  lineOfBusiness: LineOfBusiness;
  geo: string;
  approveUpToPaise: string;
  fourEyeAbovePaise: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}
