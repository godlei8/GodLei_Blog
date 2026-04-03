package cn.godlei.blogpojo.dto.site;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteConfigDTO {

    private Basic basic = new Basic();

    private Home home = new Home();

    private About about = new About();

    private Assistant assistant = new Assistant();

    public static SiteConfigDTO emptyConfig() {
        SiteConfigDTO config = new SiteConfigDTO();
        config.normalize();
        return config;
    }

    public void normalize() {
        if (basic == null) {
            basic = new Basic();
        }
        if (home == null) {
            home = new Home();
        }
        if (about == null) {
            about = new About();
        }
        if (assistant == null) {
            assistant = new Assistant();
        }
        basic.normalize();
        home.normalize();
        about.normalize();
        assistant.normalize();
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Basic {

        private String siteName = "GodLei Blog";

        private String profileAvatar = "";

        public void normalize() {
            siteName = normalizeText(siteName, "GodLei Blog");
            profileAvatar = normalizeText(profileAvatar, "");
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Home {

        private String backgroundImage = "";

        private String welcomePrefix = "WELCOME TO";

        private String welcomeHighlight = "GODLEI BLOG";

        private String noticeTitle = "公告";

        private List<String> introLines = new ArrayList<>();

        private List<String> noticeLines = new ArrayList<>();

        private List<LinkItem> socialLinks = new ArrayList<>();

        public void normalize() {
            backgroundImage = normalizeText(backgroundImage, "");
            welcomePrefix = normalizeText(welcomePrefix, "WELCOME TO");
            welcomeHighlight = normalizeText(welcomeHighlight, "GODLEI BLOG");
            noticeTitle = normalizeText(noticeTitle, "公告");
            introLines = normalizeStringList(introLines);
            noticeLines = normalizeStringList(noticeLines);
            socialLinks = normalizeLinkList(socialLinks);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class About {

        private List<String> animeImages = new ArrayList<>();

        public void normalize() {
            animeImages = normalizeStringList(animeImages);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assistant {

        private boolean enabled = true;

        private String name = "馨宝";

        private String subtitle = "站内 AI 助手";

        private String welcomeMessage = "你好，我是 **馨宝**。\n\n我可以结合当前页面内容，陪你一起梳理文章、动态和站点信息。";

        private String systemPrompt = "你是 GodLei Blog 的站内 AI 助手“馨宝”。回答时请保持自然、准确、简洁；如果页面上下文不足或事实不确定，要明确说明，不要编造。";

        private List<String> starterPrompts = new ArrayList<>(Arrays.asList(
                "帮我总结一下这页内容",
                "这篇内容最值得关注的重点是什么",
                "如果想继续深入，我应该追问什么"
        ));

        private String disclaimer = "AI 回复可能存在误差，请结合页面原文和实际情况自行判断。";

        public void normalize() {
            name = normalizeText(name, "馨宝");
            subtitle = normalizeText(subtitle, "站内 AI 助手");
            welcomeMessage = normalizeOptionalMultilineText(welcomeMessage, "你好，我是 **馨宝**。\n\n我可以结合当前页面内容，陪你一起梳理文章、动态和站点信息。");
            systemPrompt = normalizeMultilineText(systemPrompt, "你是 GodLei Blog 的站内 AI 助手“馨宝”。回答时请保持自然、准确、简洁；如果页面上下文不足或事实不确定，要明确说明，不要编造。");
            starterPrompts = normalizeStringList(starterPrompts);
            disclaimer = normalizeOptionalMultilineText(disclaimer, "AI 回复可能存在误差，请结合页面原文和实际情况自行判断。");
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LinkItem {

        private String name = "";

        private String icon = "";

        private String url = "";

        public void normalize() {
            name = normalizeText(name, "");
            icon = normalizeText(icon, "");
            url = normalizeText(url, "");
        }
    }

    private static String normalizeText(String value, String defaultValue) {
        String normalized = value == null ? "" : value.trim();
        if (!StringUtils.hasText(normalized)) {
            return defaultValue;
        }
        return normalized;
    }

    private static String normalizeMultilineText(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String normalized = value.trim();
        if (!StringUtils.hasText(normalized)) {
            return defaultValue;
        }
        return normalized;
    }

    private static String normalizeOptionalMultilineText(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value.trim();
    }

    private static List<String> normalizeStringList(List<String> source) {
        List<String> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (String item : source) {
            if (!StringUtils.hasText(item)) {
                continue;
            }
            result.add(item.trim());
        }
        return result;
    }

    private static List<LinkItem> normalizeLinkList(List<LinkItem> source) {
        List<LinkItem> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (LinkItem item : source) {
            if (item == null) {
                continue;
            }
            item.normalize();
            if (!StringUtils.hasText(item.getName()) && !StringUtils.hasText(item.getUrl())) {
                continue;
            }
            result.add(item);
        }
        return result;
    }
}
