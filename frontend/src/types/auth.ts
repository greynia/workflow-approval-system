export type UserRole = "ADMIN" | "MANAGER" | "EMPLOYEE";

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  employeeId: number;
  name: string;
  role: UserRole;
  permissions: string[];
}

export interface EmployeeResponse {
  id: number;
  employeeNo: string;
  name: string;
  email: string;
  role: UserRole;
  departmentId: number;
  managerId: number | null;
  permissions: string[];
}

// AuthUser 目前就是 LoginResponse 本身。
// 未來 store 需要多存 isAuthenticated / lastLoginAt / preferredLocale 時,
// 再展開成 `interface AuthUser extends LoginResponse { ... }`。
export type AuthUser = LoginResponse;
