import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      "/user": "http://www.myzyitzj.cn",
      "/admin": "http://localhost:8080",
      "/v3/api-docs": "http://localhost:8080",
      "/api": "http://localhost:8000",
      "/chat-stream": "http://localhost:8000",
      "/health": "http://localhost:8000",
    },
  },
});
