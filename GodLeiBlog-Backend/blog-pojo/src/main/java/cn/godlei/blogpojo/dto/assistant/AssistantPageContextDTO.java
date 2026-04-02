package cn.godlei.blogpojo.dto.assistant;

import lombok.Data;

@Data
public class AssistantPageContextDTO {

    private String pageType;

    private String route;

    private String title;

    private String summary;

    private String contentExcerpt;

    private Long momentId;

    private String currentMomentSummary;
}
