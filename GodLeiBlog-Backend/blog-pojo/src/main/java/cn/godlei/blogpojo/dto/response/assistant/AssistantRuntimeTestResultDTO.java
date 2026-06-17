package cn.godlei.blogpojo.dto.response.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssistantRuntimeTestResultDTO {

    private boolean success;

    private long durationMs;

    private Integer statusCode;

    private String message;

    private String provider;

    private String model;
}
