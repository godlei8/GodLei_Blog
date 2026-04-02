package cn.godlei.blogpojo.dto.request;

import lombok.Data;

@Data
public class MomentQueryParam {

    private Integer page = 1;

    private Integer pageSize = 10;

    private Integer status;
}
