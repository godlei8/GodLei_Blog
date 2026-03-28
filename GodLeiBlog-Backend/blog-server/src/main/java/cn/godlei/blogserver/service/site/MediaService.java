package cn.godlei.blogserver.service.site;

import cn.godlei.blogpojo.dto.response.MediaUploadResult;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {

    MediaUploadResult uploadImage(MultipartFile file, String bizType);
}
