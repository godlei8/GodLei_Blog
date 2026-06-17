package cn.godlei.blogpojo.dto.response.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AssistantRuntimeStatusDTO extends AssistantRuntimeConfigDTO {

    private boolean enabled;

    private String providerLabel;

    private boolean apiKeyConfigured;

    private String apiKeyMasked;

    private String apiKeySource;

    private String runtimeSource;

    private List<AssistantProviderPresetDTO> presets = new ArrayList<>();
}
