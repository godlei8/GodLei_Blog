package cn.godlei.blogserver.controller.admin;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.dto.response.MediaUploadResult;
import cn.godlei.blogserver.service.site.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "bizType", required = false) String bizType) {
        MediaUploadResult result = mediaService.uploadImage(file, bizType);
        log.info("后台上传图片成功，bizType={}, url={}", result.getBizType(), result.getUrl());
        return Result.success(result);
    }
}
