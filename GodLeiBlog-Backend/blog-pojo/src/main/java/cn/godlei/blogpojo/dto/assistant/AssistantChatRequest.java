package cn.godlei.blogpojo.dto.assistant;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssistantChatRequest {

    private String sessionId;

    private List<AssistantMessageDTO> messages = new ArrayList<>();

    private AssistantPageContextDTO pageContext = new AssistantPageContextDTO();
}
