package cn.godlei.blogpojo.dto.response.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantProviderPresetDTO {

    private String provider;

    private String label;

    private String defaultBaseUrl;

    private boolean custom;
}
