package cn.godlei.blogpojo.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MomentListItem {

    private Long id;

    private String content;

    private String location;

    private LocalDateTime publishTime;

    private List<String> mediaList = new ArrayList<>();
}
