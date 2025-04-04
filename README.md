# IT 之家网站

## 项目介绍

此前端项目基于 **Vue 3** 框架搭建，使用了以下技术和工具：

- **技术要点**：

  - 路由（Vue Router）
  - 组件化开发
  - `v-for` 列表渲染
  - `v-if` 条件渲染

- **工具库**：
  - **Pinia**：状态管理
  - **Axios**：HTTP 请求
  - **Element Plus**：UI 组件库

---

## 运行项目

1. 切换到项目根目录：

   ```bash
   cd xiaoyan
   ```

2. 安装项目依赖：

   ```bash
   npm install
   ```

3. 启动开发服务器：

   ```bash
   npm run dev
   ```

4. 打开浏览器，访问 `http://localhost:5173`（默认端口为 5173）。

---

## 部署项目到服务器

1. 修改 `src/axios/axiosInit.js` 中的 `baseURL` 为你服务器的地址：

   ```javascript
   const axiosInstance = axios.create({
     // 修改为你服务器的地址
     baseURL: "http://1.14.65.37:8080/",
     timeout: 5000,
   });
   ```

2. 打包项目：

   ```bash
   npm run build
   ```

3. 将生成的 `dist` 文件夹上传到服务器，并配置 Nginx：

   ```nginx
   server {
       listen 80;
       server_name your-domain.com;

       location / {
           root /path/to/dist;
           index index.html;
           try_files $uri $uri/ /index.html;
       }
   }
   ```

4. 重启 Nginx：
   ```bash
   sudo systemctl restart nginx
   ```

---

## 项目结构

```
IT之家网站/
├── public/               # 静态资源
├── src/                  # 项目源码
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   ├── store/            # Pinia 状态管理
│   ├── views/            # 页面组件
│   ├── axios/            # Axios 配置
│   ├── App.vue           # 根组件
│   └── main.js           # 项目入口
├── .env                  # 环境变量配置
├── package.json          # 项目依赖
└── README.md             # 项目说明
```

---

## 依赖列表

- **Vue 3**：前端框架
- **Pinia**：状态管理
- **Axios**：HTTP 请求库
- **Element Plus**：UI 组件库
- **Vue Router**：路由管理

## 联系方式

如有问题或建议，请联系
23 届 IT 之家会长靳玉超  
🐧 qq：2986577461
