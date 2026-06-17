package cn.godlei.blogpojo.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MomentAdminItem {

    private Long id;

    private String content;

    private String location;

    private LocalDateTime publishTime;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<String> mediaUrls = new ArrayList<>();
}
