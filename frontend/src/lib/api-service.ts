import type { AxiosRequestConfig, AxiosResponse } from "axios";
import BaseService from "./base-service";

const ApiService = {
  async fetchData<TResponse, TRequest = unknown>(
    config: AxiosRequestConfig<TRequest>
  ): Promise<TResponse> {
    const response: AxiosResponse<TResponse> =
      await BaseService.request<TResponse>(config);
    return response.data;
  },
};

export default ApiService;
