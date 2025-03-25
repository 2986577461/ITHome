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
    article_count  int         default 0,
    resource_count int         default 0,
    password       VARCHAR(20) default 123456
);
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202300109', '杨彬', '1', '物联网技术', '231班', '电子与信息', '副会长');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202300573', '靳玉超', '1', '软件技术', '232班', '电子与信息', '会长');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402134', '曾丹', '2', '软件技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402114', '吴浩然', '1', '软件技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402086', '罗艳玲', '2', '软件技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402092', '蒲政勇', '1', '软件技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402080', '廖安会', '2', '软件技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202403128', '徐袁丽', '2', '网络技术', '241班', '电子与信息', '干事');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402968', '拉尔阿作莫', '2', '物联网技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202403040', '钟妹宏', '2', '物联网技术', '241班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202403347', '唐强', '1', '电子与信息', '242班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202404641', '刘兴泉', '1', '人力资源', '242班', '旅游与管理', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402123', '徐铭阳', '1', '软件技术', '242班', '电子与信息', '干事');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402069', '吉巴科哈', '1', '软件技术', '242班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402119', '肖志豪', '1', '软件技术', '242班', '电子与信息', '干事');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402059', '龚胜利', '1', '软件技术', '242班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402061', '古驰', '1', '软件技术', '242班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402095', '尚超', '1', '软件技术', '242班', '电子与信息', '学员');
INSERT INTO it_student(student_id, name, sex, major, class, academy, position)
values ('202402985', '刘宇翔', '1', '物联网技术', '242班', '电子与信息', '学员');

CREATE TABLE resources
(
    id           int AUTO_INCREMENT primary key,
    head         varchar(200)  not null,
    introduce      varchar(4000) NOT NULL,
    file_name       varchar(200)   not null,
    student_name varchar(20) not null,
    student_id   varchar(40)   not null,
    release_date date          not null,
    release_time time          not null
);
CREATE TABLE article
(
    id           INT AUTO_INCREMENT primary key,
    type         varchar(40) not null ,
    head         varchar(100) not null,
    content      varchar(4000) not null,
    author_id   varchar(40)   not null,
    name       varchar(20)   not null,
    release_date date          not null,
    release_time time          not null,
    update_date  date          not null,
    update_time  time          not null
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