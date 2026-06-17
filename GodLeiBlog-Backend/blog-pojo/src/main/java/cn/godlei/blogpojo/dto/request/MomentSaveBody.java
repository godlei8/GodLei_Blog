package cn.godlei.blogpojo.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class MomentSaveBody {

    private Long id;

    private String content;

    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishTime;

    private Integer status = 0;

    private List<String> mediaUrls = new ArrayList<>();
}
