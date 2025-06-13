# USE YOUR DATABASE;

CREATE TABLE it_student
(
    student_id     VARCHAR(20) PRIMARY KEY,
    name           VARCHAR(20)      NOT NULL,
    sex            ENUM ('男','女') NOT NULL,
    major          VARCHAR(20)      NOT NULL,
    class          VARCHAR(20)      NOT NULL,
    academy        VARCHAR(20)      NOT NULL,
    position       varchar(20)      NOT NULL,
    article_count  int default 0,
    resource_count int default 0,
    password       VARCHAR(100)     NOT NULL
);
INSERT INTO it_student(student_id, name, sex, major, class, academy, position, article_count, resource_count, password)
values ('1', '测试', '1', '软件技术', '232班', '电子与信息', '会长', 0, 0,
        '$2a$10$8OVqXgG4u2RNTSFD0NB2KOBRn8ZAv3EX4nxFQfmJUcqRLRsAoeVYO');

CREATE TABLE resources
(
    id           int AUTO_INCREMENT primary key,
    head         varchar(200)  not null,
    introduce    varchar(4000) NOT NULL,
    file_name    varchar(200)  not null,
    student_name varchar(20)   not null,
    student_id   varchar(40)   not null,
    release_date date          not null,
    release_time time          not null
);
CREATE TABLE article
(
    id                BIGINT AUTO_INCREMENT primary key,
    type              varchar(40)   not null,
    head              varchar(100)  not null,
    content           varchar(4000) not null,
    author_id         varchar(40)   not null,
    author              varchar(20)   not null,
    release_date_time DATETIME      not null,
    update_date_time  datetime      not null
);

CREATE TABLE newcomer
(
    id         INT PRIMARY KEY AUTO_INCREMENT,
    student_id VARCHAR(40)      NOT NULL,
    name       VARCHAR(20)      NOT NULL,
    sex        ENUM ('男','女') NOT NULL,
    major      VARCHAR(20)      NOT NULL,
    class      VARCHAR(20)      NOT NULL,
    academy    VARCHAR(20)      NOT NULL,
    introduce  VARCHAR(2000)    NOT NULL

);
SET GLOBAL max_allowed_packet = 64 * 1024 * 1024;
# //设置 SQL 查询包大小