drop database if exists ithome;
CREATE DATABASE if not exists `ithome` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

use ithome;
CREATE TABLE it_student
(
    id             bigint PRIMARY KEY,
    student_id     int              not null,
    name           VARCHAR(20)      NOT NULL,
    sex            ENUM ('男','女') NOT NULL,
    major          VARCHAR(20)      NOT NULL,
    class_name     VARCHAR(20)      NOT NULL,
    academy        VARCHAR(20)      NOT NULL,
    position       varchar(20)      NOT NULL,
    avatar_id      bigint           not null,
    article_count  int              not null default 0,
    resource_count int              not null default 0,
    password       VARCHAR(100)     NOT NULL,
    deleted        boolean          not null default 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
INSERT INTO it_student(id, student_id, name, sex, major, class_name, academy, position, avatar_id,
                       password)
values (1, '202402059', '龚胜利', '1', '软件技术', '242班', '人工智能', '会长',
        0, '$2a$10$mdjdwZXRLhqaMgk7mBBQ/.7JpD5kvCUCAsrhdJaprlr/7BVZ.fLq.');
INSERT INTO it_student(id, student_id, name, sex, major, class_name, academy, position, avatar_id,
                       password)
    VALUE (2, '202300573', '超超', '1', '软件技术', '232班', '人工智能', '会长',
           1, '$2a$10$mdjdwZXRLhqaMgk7mBBQ/.7JpD5kvCUCAsrhdJaprlr/7BVZ.fLq.');

CREATE TABLE resources
(
    id                    bigint primary key,
    head                  varchar(100) not null,
    introduce             varchar(500) NOT NULL,
    student_id            int          not null,
    student_file_cover_id bigint       not null,
    student_file_file_id  bigint       not null,
    release_date_time     datetime     not null,
    deleted               tinyint(1)   not null default 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
CREATE TABLE article
(
    id                bigint primary key,
    type              tinyint(1)     not null,
    head              varchar(100)   not null,
    content           varchar(10000) not null,
    student_id        int            not null,
    release_date_time DATETIME       not null,
    updated_date_time datetime       not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE newcomer
(
    id                    bigint PRIMARY KEY,
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

create table student_file
(
    id               bigint primary key,
    student_id       int                 not null,
    original_name    varchar(200)        not null,
    object_name      varchar(200) unique not null,
    file_url         varchar(200)        not null,
    file_size        bigint              not null,
    file_type        varchar(80)         not null,
    create_date_time datetime            not null,
    deleted          tinyint(1) default 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

;