package cn.godlei.blogserver.controller.admin;

import cn.godlei.blogcommon.util.Result;
import cn.godlei.blogpojo.dto.request.MomentQueryParam;
import cn.godlei.blogpojo.dto.request.MomentSaveBody;
import cn.godlei.blogpojo.dto.response.MomentAdminItem;
import cn.godlei.blogpojo.dto.response.PageResult;
import cn.godlei.blogserver.service.moment.MomentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/moments")
public class AdminMomentsController {

    private final MomentsService momentsService;

    @GetMapping("/list")
    public Result list(MomentQueryParam queryParam) {
        PageResult<MomentAdminItem> result = momentsService.listAdmin(queryParam);
        return Result.success(result);
    }

    @PostMapping("/add")
    public Result add(@RequestBody MomentSaveBody body) {
        momentsService.add(body);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody MomentSaveBody body) {
        momentsService.update(body);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result delete(Long id) {
        momentsService.delete(id);
        return Result.success();
    }
}
