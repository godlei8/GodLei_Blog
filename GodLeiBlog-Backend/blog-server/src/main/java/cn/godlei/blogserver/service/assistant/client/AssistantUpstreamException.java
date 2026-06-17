package cn.godlei.blogserver.service.assistant.client;

import lombok.Getter;

@Getter
public class AssistantUpstreamException extends RuntimeException {

    private final Integer statusCode;

    private final String upstreamSummary;

    public AssistantUpstreamException(String message, Integer statusCode, String upstreamSummary) {
        super(message);
        this.statusCode = statusCode;
        this.upstreamSummary = upstreamSummary;
    }

    public AssistantUpstreamException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.upstreamSummary = "";
    }
}
