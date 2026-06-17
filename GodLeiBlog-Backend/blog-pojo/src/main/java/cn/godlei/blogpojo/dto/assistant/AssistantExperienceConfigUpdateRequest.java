package cn.godlei.blogpojo.dto.assistant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssistantExperienceConfigUpdateRequest {

    private boolean enabled = true;

    private String name;

    private String subtitle;

    private String welcomeMessage;

    private List<String> starterPrompts = new ArrayList<>();

    private String disclaimer;

    private String systemPrompt;
}
