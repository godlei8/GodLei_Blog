package cn.godlei.blogserver.controller.user;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.dto.request.MomentQueryParam;
import cn.godlei.blogpojo.dto.response.MomentListItem;
import cn.godlei.blogpojo.dto.response.PageResult;
import cn.godlei.blogserver.service.moment.MomentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/moments")
public class MomentsController {

    private final MomentsService momentsService;

    @GetMapping("/list")
    public Result list(MomentQueryParam queryParam) {
        PageResult<MomentListItem> result = momentsService.listPublic(queryParam);
        return Result.success(result);
    }

    @GetMapping("/get")
    public Result get(Long id) {
        return Result.success(momentsService.getPublic(id));
    }
}
