import type {
  EmployeeResponse,
  LoginRequest,
  LoginResponse,
} from "@/types/auth";
import type { EmployeeSummary } from "@/types/common";

type MockEmployeeAccount = EmployeeResponse & {
  password: string;
};

export const mockEmployeeAccounts: MockEmployeeAccount[] = [
  {
    id: 1,
    employeeNo: "A0001",
    name: "Eva Admin",
    email: "eva.admin@company.com",
    password: "password123",
    role: "ADMIN",
    departmentId: 1,
    managerId: null,
  },
  {
    id: 2,
    employeeNo: "M0001",
    name: "Mina Manager",
    email: "mina.manager@company.com",
    password: "password123",
    role: "MANAGER",
    departmentId: 2,
    managerId: 1,
  },
  {
    id: 3,
    employeeNo: "E0001",
    name: "Alice Chen",
    email: "alice.chen@company.com",
    password: "password123",
    role: "EMPLOYEE",
    departmentId: 2,
    managerId: 2,
  },
  {
    id: 4,
    employeeNo: "E0002",
    name: "Bob Wang",
    email: "bob.wang@company.com",
    password: "password123",
    role: "EMPLOYEE",
    departmentId: 2,
    managerId: 2,
  },
  {
    id: 5,
    employeeNo: "E0003",
    name: "Carol Liu",
    email: "carol.liu@company.com",
    password: "password123",
    role: "EMPLOYEE",
    departmentId: 2,
    managerId: 2,
  },
];

export function findMockEmployeeByCredentials({
  email,
  password,
}: LoginRequest): MockEmployeeAccount | undefined {
  return mockEmployeeAccounts.find(
    (employee) => employee.email === email && employee.password === password
  );
}

export function findMockEmployeeById(id: number): MockEmployeeAccount | undefined {
  return mockEmployeeAccounts.find((employee) => employee.id === id);
}

export function toLoginResponse(employee: MockEmployeeAccount): LoginResponse {
  return {
    employeeId: employee.id,
    name: employee.name,
    role: employee.role,
  };
}

export function toEmployeeSummary(employee: MockEmployeeAccount): EmployeeSummary {
  return {
    id: employee.id,
    employeeNo: employee.employeeNo,
    name: employee.name,
  };
}

export function toEmployeeResponse(
  employee: MockEmployeeAccount
): EmployeeResponse {
  return {
    id: employee.id,
    employeeNo: employee.employeeNo,
    name: employee.name,
    email: employee.email,
    role: employee.role,
    departmentId: employee.departmentId,
    managerId: employee.managerId,
  };
}
