import ApiService from "@/lib/api-service";
import type {
  EmployeeResponse,
  LoginRequest,
  LoginResponse,
} from "@/types/auth";

const AuthService = {
  signIn(data: LoginRequest): Promise<LoginResponse> {
    return ApiService.fetchData<LoginResponse, LoginRequest>({
      url: "/auth/login",
      method: "POST",
      data,
    });
  },

  signOut(): Promise<void> {
    return ApiService.fetchData<void>({
      url: "/auth/logout",
      method: "POST",
    });
  },

  getMe(): Promise<EmployeeResponse> {
    return ApiService.fetchData<EmployeeResponse>({
      url: "/auth/me",
      method: "GET",
    });
  },
};

export default AuthService;
