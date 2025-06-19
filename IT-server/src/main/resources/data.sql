# USE YOUR DATABASE;
drop database ITHome;
create database ITHome;
use ITHome;
CREATE TABLE it_student
(
    id             bigint PRIMARY KEY,
    name           VARCHAR(20)      NOT NULL,
    sex            ENUM ('男','女') NOT NULL,
    major          VARCHAR(20)      NOT NULL,
    class_name     VARCHAR(20)      NOT NULL,
    academy        VARCHAR(20)      NOT NULL,
    position       varchar(20)      NOT NULL,
    article_count  bigint default 0,
    resource_count bigint default 0,
    password       VARCHAR(100)     NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
INSERT INTO it_student(id, name, sex, major, class_name, academy, position, article_count, resource_count, password)
values ('1', '测试', '1', '软件技术', '232班', '电子与信息', '副会长', 0, 0,
        '$2a$10$8OVqXgG4u2RNTSFD0NB2KOBRn8ZAv3EX4nxFQfmJUcqRLRsAoeVYO');
INSERT INTO it_student(id, name, sex, major, class_name, academy, position, article_count, resource_count, password)
    VALUE ('202300573', '超超', '1', '软件技术', '232班', '电子与信息', '会长', 0, 0,
           '$2a$10$8OVqXgG4u2RNTSFD0NB2KOBRn8ZAv3EX4nxFQfmJUcqRLRsAoeVYO');

CREATE TABLE resources
(
    id                bigint AUTO_INCREMENT primary key,
    head              varchar(200)  not null,
    introduce         varchar(4000) NOT NULL,
    student_name      varchar(20)   not null,
    student_id        bigint        not null,
    release_date_time datetime      not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;;
CREATE TABLE article
(
    id                BIGINT AUTO_INCREMENT primary key,
    type              varchar(40)   not null,
    head              varchar(100)  not null,
    content           varchar(4000) not null,
    author_id         bigint        not null,
    author            varchar(20)   not null,
    release_date_time DATETIME      not null,
    updated_date_time datetime      not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;;
INSERT INTO `article`
VALUES (1, 'css', 'test',
        '<p><span style=\"font-family: 黑体;\">这是测试用例</span><span style=\"font-size: 13px; font-family: 黑体;\">发生的</span></p><p><span style=\"font-size: 22px; font-family: 黑体;\">发生的🙃</span></p>',
        '202300573', '靳玉超', '2025-06-12 08:48:32', '2025-06-12 21:25:37'),
       (2, 'html', 'hh', '<p>fasd<span style=\"color: rgb(0, 0, 0);\">fasdfasdfasdfasdfasdfasd</span></p>', '202300573',
        '靳玉超', '2025-06-12 08:50:39', '2025-06-12 08:50:39'),
       (3, 'java', 'fast',
        '<p>fasdfasdfa<span style=\"color: rgb(0, 0, 0);\">fasdfasdfafasdfasdfafasdfasdfa</span></p>', '202300573',
        '靳玉超', '2025-06-12 08:51:17', '2025-06-12 08:51:17'),
       (4, 'css', 'fff',
        '<p>a<span style=\"color: rgb(0, 0, 0);\">aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</span></p>',
        '202300573', '靳玉超', '2025-06-12 08:52:25', '2025-06-12 08:52:25'),
       (5, 'mysql', 'mysql基础多表查询',
        '<p style=\"text-align: left;\"><span style=\"font-size: 19px; font-family: 标楷体;\">select t1.department_name \'部门名称\' t2.name \'员工姓名\'</span></p><p style=\"text-align: left;\"><span style=\"font-size: 19px; font-family: 标楷体;\">from department t1 join employees t2</span></p><p style=\"text-align: left;\"><span style=\"font-size: 19px; font-family: 标楷体;\">where t2.department_id=t1.department_id;</span></p>',
        '202300573', '靳玉超', '2025-06-12 08:56:41', '2025-06-12 08:56:41');

CREATE TABLE newcomer
(
    id                    bigint PRIMARY KEY AUTO_INCREMENT,
    student_id            bigint           NOT NULL,
    name                  VARCHAR(20)      NOT NULL,
    sex                   ENUM ('男','女') NOT NULL,
    major                 VARCHAR(20)      NOT NULL,
    class_name                 VARCHAR(20)      NOT NULL,
    academy               VARCHAR(20)      NOT NULL,
    introduce             VARCHAR(2000)    NOT NULL,
    application_date_time datetime         not null

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;;
SET GLOBAL max_allowed_packet = 64 * 1024 * 1024;
# //设置 SQL 查询包大小