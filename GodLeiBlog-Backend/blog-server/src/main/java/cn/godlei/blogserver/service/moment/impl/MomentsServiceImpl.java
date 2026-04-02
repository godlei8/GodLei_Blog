package cn.godlei.blogserver.service.moment.impl;

import cn.godlei.blogpojo.dto.request.MomentQueryParam;
import cn.godlei.blogpojo.dto.request.MomentSaveBody;
import cn.godlei.blogpojo.dto.response.MomentAdminItem;
import cn.godlei.blogpojo.dto.response.MomentListItem;
import cn.godlei.blogpojo.dto.response.PageResult;
import cn.godlei.blogpojo.entity.BlogMoment;
import cn.godlei.blogpojo.entity.BlogMomentMedia;
import cn.godlei.blogserver.mapper.BlogMomentMapper;
import cn.godlei.blogserver.mapper.BlogMomentMediaMapper;
import cn.godlei.blogserver.service.moment.MomentsService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentsServiceImpl implements MomentsService {

    private final BlogMomentMapper blogMomentMapper;

    private final BlogMomentMediaMapper blogMomentMediaMapper;

    @Override
    public PageResult<MomentListItem> listPublic(MomentQueryParam queryParam) {
        MomentQueryParam safeQuery = normalizeQuery(queryParam);
        if (safeQuery.getStatus() == null) {
            safeQuery.setStatus(1);
        }
        PageHelper.startPage(safeQuery.getPage(), safeQuery.getPageSize());
        List<BlogMoment> moments = blogMomentMapper.listPublic(safeQuery);
        Page<BlogMoment> page = (Page<BlogMoment>) moments;
        return new PageResult<>(page.getTotal(), attachPublicMedia(page.getResult()));
    }

    @Override
    public MomentListItem getPublic(Long id) {
        BlogMoment moment = requireMoment(id);
        if (!Integer.valueOf(1).equals(moment.getStatus())) {
            return null;
        }
        if (moment.getPublishTime() != null && moment.getPublishTime().isAfter(LocalDateTime.now())) {
            return null;
        }
        List<MomentListItem> items = attachPublicMedia(Collections.singletonList(moment));
        return items.isEmpty() ? null : items.get(0);
    }

    @Override
    public PageResult<MomentAdminItem> listAdmin(MomentQueryParam queryParam) {
        MomentQueryParam safeQuery = normalizeQuery(queryParam);
        PageHelper.startPage(safeQuery.getPage(), safeQuery.getPageSize());
        List<BlogMoment> moments = blogMomentMapper.listAdmin(safeQuery);
        Page<BlogMoment> page = (Page<BlogMoment>) moments;
        return new PageResult<>(page.getTotal(), attachAdminMedia(page.getResult()));
    }

    @Override
    @Transactional
    public void add(MomentSaveBody body) {
        BlogMoment moment = buildMomentEntity(body, null);
        blogMomentMapper.insert(moment);
        syncMedia(moment.getId(), body == null ? Collections.emptyList() : body.getMediaUrls());
        log.info("Create moment success, id={}", moment.getId());
    }

    @Override
    @Transactional
    public void update(MomentSaveBody body) {
        if (body == null || body.getId() == null) {
            throw new IllegalArgumentException("动态 ID 不能为空");
        }
        requireMoment(body.getId());
        BlogMoment moment = buildMomentEntity(body, body.getId());
        blogMomentMapper.update(moment);
        syncMedia(moment.getId(), body.getMediaUrls());
        log.info("Update moment success, id={}", moment.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireMoment(id);
        blogMomentMediaMapper.deleteByMomentId(id);
        blogMomentMapper.delete(id);
        log.info("Delete moment success, id={}", id);
    }

    private MomentQueryParam normalizeQuery(MomentQueryParam queryParam) {
        MomentQueryParam safeQuery = queryParam == null ? new MomentQueryParam() : queryParam;
        if (safeQuery.getPage() == null || safeQuery.getPage() < 1) {
            safeQuery.setPage(1);
        }
        if (safeQuery.getPageSize() == null || safeQuery.getPageSize() < 1) {
            safeQuery.setPageSize(10);
        }
        return safeQuery;
    }

    private BlogMoment buildMomentEntity(MomentSaveBody body, Long id) {
        if (body == null || !StringUtils.hasText(body.getContent())) {
            throw new IllegalArgumentException("动态内容不能为空");
        }
        BlogMoment moment = new BlogMoment();
        moment.setId(id);
        moment.setContent(body.getContent().trim());
        moment.setLocation(normalizeText(body.getLocation()));
        moment.setPublishTime(body.getPublishTime() == null ? LocalDateTime.now() : body.getPublishTime());
        moment.setStatus(normalizeStatus(body.getStatus()));
        return moment;
    }

    private void syncMedia(Long momentId, List<String> mediaUrls) {
        blogMomentMediaMapper.deleteByMomentId(momentId);
        List<String> normalized = normalizeMediaUrls(mediaUrls);
        if (normalized.isEmpty()) {
            return;
        }
        List<BlogMomentMedia> mediaList = new ArrayList<>();
        for (int i = 0; i < normalized.size(); i++) {
            BlogMomentMedia media = new BlogMomentMedia();
            media.setMomentId(momentId);
            media.setMediaUrl(normalized.get(i));
            media.setSort(i + 1);
            mediaList.add(media);
        }
        blogMomentMediaMapper.batchInsert(mediaList);
    }

    private BlogMoment requireMoment(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("动态 ID 不能为空");
        }
        BlogMoment moment = blogMomentMapper.getById(id);
        if (moment == null) {
            throw new IllegalArgumentException("动态不存在或已删除");
        }
        return moment;
    }

    private List<MomentListItem> attachPublicMedia(List<BlogMoment> moments) {
        Map<Long, List<String>> mediaMap = loadMediaMap(moments);
        return moments.stream().map(moment -> {
            MomentListItem item = new MomentListItem();
            item.setId(moment.getId());
            item.setContent(moment.getContent());
            item.setLocation(moment.getLocation());
            item.setPublishTime(moment.getPublishTime());
            item.setMediaList(new ArrayList<>(mediaMap.getOrDefault(moment.getId(), Collections.emptyList())));
            return item;
        }).collect(Collectors.toList());
    }

    private List<MomentAdminItem> attachAdminMedia(List<BlogMoment> moments) {
        Map<Long, List<String>> mediaMap = loadMediaMap(moments);
        return moments.stream().map(moment -> {
            MomentAdminItem item = new MomentAdminItem();
            item.setId(moment.getId());
            item.setContent(moment.getContent());
            item.setLocation(moment.getLocation());
            item.setPublishTime(moment.getPublishTime());
            item.setStatus(moment.getStatus());
            item.setCreatedAt(moment.getCreatedAt());
            item.setUpdatedAt(moment.getUpdatedAt());
            item.setMediaUrls(new ArrayList<>(mediaMap.getOrDefault(moment.getId(), Collections.emptyList())));
            return item;
        }).collect(Collectors.toList());
    }

    private Map<Long, List<String>> loadMediaMap(List<BlogMoment> moments) {
        if (moments == null || moments.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = moments.stream().map(BlogMoment::getId).collect(Collectors.toList());
        List<BlogMomentMedia> mediaList = blogMomentMediaMapper.listByMomentIds(ids);
        Map<Long, List<String>> mediaMap = new LinkedHashMap<>();
        for (BlogMomentMedia media : mediaList) {
            mediaMap.computeIfAbsent(media.getMomentId(), key -> new ArrayList<>()).add(media.getMediaUrl());
        }
        return mediaMap;
    }

    private List<String> normalizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaUrls.stream()
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private Integer normalizeStatus(Integer status) {
        return Integer.valueOf(1).equals(status) ? 1 : 0;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }
}
