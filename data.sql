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
    id                    int AUTO_INCREMENT primary key,
    head                  varchar(100) not null,
    introduce             varchar(500) NOT NULL,
    student_id            int          not null,
    student_file_cover_id int          not null,
    student_file_file_id  int          not null,
    release_date_time     datetime     not null,
    deleted               tinyint(1)   not null default 0
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

create table student_medals
(
    id               int primary key auto_increment,
    head             varchar(20) not null,
    student_id       int         not null,
    grade            tinyint(1)  not null,
    student_file_id  int         not null,
    create_date_time datetime    not null,
    deleted          tinyint(1)  not null default 0
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


create table ai_dialog_session
(
    id                    int primary key auto_increment,
    title                 varchar(20),
    student_id            int        not null,
    create_date_time      datetime   not null,
    last_active_date_time datetime   not null,
    deleted               tinyint(1) not null default 0
)ENGINE = InnoDB
 DEFAULT CHARSET = utf8mb4
 COLLATE = utf8mb4_unicode_ci;
create table ai_dialog
(
    id               int primary key auto_increment,
    session_id         int         not null,
    sender_type      varchar(20) not null,
    content          text        not null,
    create_date_time datetime    not null,
    deleted          tinyint(1)  not null default 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

create table chat_dialog
(
    id               int primary key auto_increment,
    sender_id        int           not null,
    receiver_id      int           not null,
    content          varchar(3000) not null,
    create_date_time datetime      not null
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


create table student_file
(
    id               int primary key auto_increment,
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
