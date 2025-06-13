# IT之家后端项目

## 项目介绍

本项目基于 **Spring Boot 3** 框架开发，用于接收并响应前端的 Axios 请求。主要特点如下：

- **令牌访问**：使用了Sa-Token框架简化了token的传输和校验。
- **身份验证**：使用 AOP 切面实现身份验证，简化了 `Service` 层的开发。
- **数据访问**：使用 **MyBatis-Plus** 框架，简化了 `Mapper` 的开发。
- **业务逻辑**：`Service` 层根据业务复杂度调用 MyBatis-Plus 或自定义 `Mapper` 接口的方法。

---

## 接口文档

位于  `localhost:8080/doc.html`
如果部署了云服务器，IP位和端口号可能不同

---

## 运行项目

1. **初始化数据库**：
    - 运行 `resources` 目录下的 `data.sql` 脚本，初始化数据库表和数据。

2. **修改配置文件**：
    - 修改 `IT-server/src/main/resources` 目录下的 `application.yml` 文件
    - 或者导入`application-dev.yml`放在和`application.yml`同一路径

3. **启动项目**：
    - 运行 `Application.java` 中的 `main` 方法，启动 Spring Boot 项目。

4. **数据测试**;
    - data.sql默认导入两个测试账号，账号分别为`1`和`202300573`
      密码都是123456

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

