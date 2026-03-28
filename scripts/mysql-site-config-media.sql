use godleiblog;

create table if not exists blog_site_setting
(
    setting_key   varchar(64)                        not null primary key comment '配置项键',
    setting_value longtext                           not null comment '配置项值(JSON)',
    setting_desc  varchar(255)                       null comment '配置说明',
    created_at    datetime default CURRENT_TIMESTAMP not null,
    updated_at    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
)
    comment '站点设置表' collate = utf8mb4_unicode_ci;

create table if not exists blog_media_file
(
    id            bigint unsigned auto_increment primary key,
    biz_type      varchar(32)                         not null comment '业务类型(site/post/common...)',
    storage_type  varchar(32)                         not null comment '存储方式(local/oss)',
    original_name varchar(255)                        not null comment '原始文件名',
    stored_name   varchar(255)                        not null comment '存储文件名',
    extension     varchar(32)                         null comment '文件扩展名',
    content_type  varchar(128)                        null comment '文件MIME类型',
    file_size     bigint unsigned                     not null default 0 comment '文件大小(字节)',
    relative_path varchar(512)                        not null comment '相对存储路径',
    access_url    varchar(1024)                       not null comment '访问URL',
    created_at    datetime default CURRENT_TIMESTAMP  not null
)
    comment '媒体文件表' collate = utf8mb4_unicode_ci;
