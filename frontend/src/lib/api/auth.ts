import { apiClient } from "./client";
import { isSessionPrincipal } from "@/lib/session";

export const DISCORD_LOGIN_PATH = "/bff/oauth2/authorization/discord";

export const authApi = {
  getSession: () => apiClient.get("/bff/auth/session", isSessionPrincipal),
  logout: () => apiClient.deleteEmpty("/bff/auth/logout")
};
