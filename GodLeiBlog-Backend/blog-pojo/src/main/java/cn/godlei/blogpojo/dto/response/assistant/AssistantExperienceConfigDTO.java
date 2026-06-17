package cn.godlei.blogpojo.dto.response.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantExperienceConfigDTO {

    private boolean enabled;

    private String name;

    private String subtitle;

    private String welcomeMessage;

    private List<String> starterPrompts = new ArrayList<>();

    private String disclaimer;

    private String systemPrompt;
}
