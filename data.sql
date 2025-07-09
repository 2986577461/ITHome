drop database ITHome;
CREATE DATABASE IF NOT EXISTS `ITHome` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
use ITHome;
CREATE TABLE it_student
(
    id             int PRIMARY KEY auto_increment,
    student_id     int              not null,
    name           VARCHAR(20)      NOT NULL,
    sex            ENUM ('男','女') NOT NULL,
    major          VARCHAR(20)      NOT NULL,
    class_name     VARCHAR(20)      NOT NULL,
    academy        VARCHAR(20)      NOT NULL,
    position       varchar(20)      NOT NULL,
    article_count  int              not null default 0,
    resource_count int              not null default 0,
    password       VARCHAR(100)     NOT NULL,
    deleted        boolean          not null default 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
INSERT INTO it_student(student_id, name, sex, major, class_name, academy, position, article_count, resource_count,
                       password)
values ('202300156', '程小伟', '1', '软件技术', '232班', '电子与信息', '副会长', 0, 0,
        '$2a$10$8OVqXgG4u2RNTSFD0NB2KOBRn8ZAv3EX4nxFQfmJUcqRLRsAoeVYO');
INSERT INTO it_student(student_id, name, sex, major, class_name, academy, position, article_count, resource_count,
                       password)
    VALUE ('202300573', '超超', '1', '软件技术', '232班', '电子与信息', '会长', 0, 0,
           '$2a$10$8OVqXgG4u2RNTSFD0NB2KOBRn8ZAv3EX4nxFQfmJUcqRLRsAoeVYO');

CREATE TABLE resources
(
    id                int AUTO_INCREMENT primary key,
    head              varchar(100) not null,
    introduce         varchar(500) NOT NULL,
    student_id        int          not null,
    file_name         varchar(50)  not null,
    file_url          varchar(300) not null,
    cover_url         varchar(300) not null,
    release_date_time datetime     not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE TABLE article
(
    id                INT AUTO_INCREMENT primary key,
    type              tinyint(1)    not null,
    head              varchar(100)  not null,
    content           varchar(5000) not null,
    student_id        int           not null,
    release_date_time DATETIME      not null,
    updated_date_time datetime      not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
INSERT INTO `article`
VALUES (1, 1, 'test',
        '<p><span style=\"font-family: 黑体;\">这是测试用例</span><span style=\"font-size: 13px; font-family: 黑体;\">发生的</span></p><p><span style=\"font-size: 22px; font-family: 黑体;\">发生的🙃</span></p>',
        '202300573', '2025-06-12 08:48:32', '2025-06-12 21:25:37'),
       (2, 2, 'hh', '<p>fasd<span style=\"color: rgb(0, 0, 0);\">fasdfasdfasdfasdfasdfasd</span></p>', '202300573',
        '2025-06-12 08:50:39', '2025-06-12 08:50:39'),
       (3, 3, 'fast',
        '<p>fasdfasdfa<span style=\"color: rgb(0, 0, 0);\">fasdfasdfafasdfasdfafasdfasdfa</span></p>', '202300573',
        '2025-06-12 08:51:17', '2025-06-12 08:51:17'),
       (4, 2, 'fff',
        '<p>a<span style=\"color: rgb(0, 0, 0);\">aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</span></p>',
        '202300573', '2025-06-12 08:52:25', '2025-06-12 08:52:25'),
       (5, 4, 'mysql基础多表查询',
        '<p style=\"text-align: left;\"><span style=\"font-size: 19px; font-family: 标楷体;\">select t1.department_name \'部门名称\' t2.name \'员工姓名\'</span></p><p style=\"text-align: left;\"><span style=\"font-size: 19px; font-family: 标楷体;\">from department t1 join employees t2</span></p><p style=\"text-align: left;\"><span style=\"font-size: 19px; font-family: 标楷体;\">where t2.department_id=t1.department_id;</span></p>',
        '202300573', '2025-06-12 08:56:41', '2025-06-12 08:56:41');

CREATE TABLE newcomer
(
    id                    int PRIMARY KEY AUTO_INCREMENT,
    student_id            int              NOT NULL,
    name                  varchar(20)      not null,
    sex                   ENUM ('男','女') NOT NULL,
    major                 VARCHAR(20)      NOT NULL,
    class_name            VARCHAR(20)      NOT NULL,
    academy               VARCHAR(20)      NOT NULL,
    introduce             VARCHAR(2000)    NOT NULL,
    application_date_time datetime         not null

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

create table student_medal
(
    id         int primary key auto_increment,
    head       varchar(20)  not null,
    student_id int          not null,
    medal_url  varchar(100) not null,
    introduce  varchar(200) not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE career_student
(
    id                  int primary key auto_increment,
    student_id          int          not null,
    familiar_technology varchar(100) not null,
    content             varchar(500) not null,
    contact_type        tinyint(1)   not null,
    contact_way         varchar(50)  not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

create table ai_dialog
(
    id               int primary key auto_increment,
    sender           varchar(20)   not null,
    content          varchar(3000) not null,
    create_date_time datetime      not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

create table chat_dialog
(
    id               int primary key auto_increment,
    sender           varchar(20)   not null,
    receiver         varchar(20)   not null,
    content          varchar(3000) not null,
    create_date_time datetime      not null
)ENGINE = InnoDB
 DEFAULT CHARSET = utf8mb4
 COLLATE = utf8mb4_unicode_ci;
