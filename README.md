
# IT之家后端项目

## 项目介绍

本项目基于 **Spring Boot 3** 框架开发，用于接收并响应前端的 Axios 请求。主要特点如下：

- **令牌访问**：使用了Sa-Token框架简化了token的传输和校验。
- **身份验证**：使用 AOP 切面实现身份验证，简化了 `Service` 层的开发。
- **数据访问**：使用 **MyBatis-Plus** 框架，简化了 `Mapper` 的开发。
- **业务逻辑**：`Service` 层根据业务复杂度调用 MyBatis-Plus 或自定义 `Mapper` 接口的方法。
    


## 项目结构

```
IT之家后端/
├── src/main/java/
│   ├── annotation/       # 自定义注解（如身份验证注解）
│   ├── aspect/           # AOP 切面（如身份验证切面）
│   ├── config/           # 配置类（如 WebConfig）
│   ├── controller/       # 控制器层，处理 HTTP 请求
│   ├── dto/              # 数据传输对象（DTO）
│   ├── exception/        # 自定义异常类
│   ├── globalException/  # 全局异常处理
│   ├── interceptor/      # 拦截器（如身份验证拦截器）
│   ├── mapper/           # MyBatis Mapper 接口
│   ├── pojo/             # 实体类（POJO）
│   ├── service/          # 服务层，实现业务逻辑
│   └── Application.java  # 项目启动类
├── src/main/resources/
│   ├── application.yml   # 项目配置文件
│   └── data.sql          # 数据库初始化脚本
├── pom.xml               # Maven 依赖配置文件
└── README.md             # 项目说明文档
```

---

## 运行项目

1. **初始化数据库**：
    - 运行 `resources` 目录下的 `data.sql` 脚本，初始化数据库表和数据。

2. **修改配置文件**：
    - 修改 `resources` 目录下的 `application.yml` 文件：
        - 更新数据库连接信息（如 `url`、`username`、`password`）。
        - 配置 `resources-root-directory` 的路径（如果需要）。

3. **启动项目**：
    - 运行 `Application.java` 中的 `main` 方法，启动 Spring Boot 项目。

---

## 部署项目到服务器

1. **导入数据库**：
    - 将 `data.sql` 导入到服务器的数据库中。

2. **打包项目**：
    - 使用 Maven 工具将项目打包为 JAR 文件：
      ```bash
      mvn clean package
      ```

3. **上传 JAR 文件**：
    - 将生成的 JAR 文件上传到服务器。

4. **运行项目**：
    - 在服务器中，切换到 JAR 文件所在的目录，运行以下命令：
      ```bash
      nohup java -jar 文件名
      ```
    - 按 `Ctrl + D` 退出终端，项目将在后台运行。

---

## 技术栈

- **Spring Boot 3**：后端框架
- **MyBatis-Plus**：数据访问框架
- **AOP**：面向切面编程（身份验证）
- **MySQL**：数据库
- **Maven**：项目构建工具

---


## 联系方式

如有问题或建议，请联系
23 届 IT 之家会长靳玉超  
🐧 qq：2986577461

