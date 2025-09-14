import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

export default defineConfig({
  plugins: [vue()], // 合并插件配置
  build: {
    outDir: "static_pages",
  },
  server: {
    host: "0.0.0.0", // 允许外部 IP 访问
    port: 5173, // 可选，指定端口号，默认为 5173
    strictPort: true, // 如果端口被占用，强制使用指定端口
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"), // 确保 @ 指向 src 目录
    },
  },
});
