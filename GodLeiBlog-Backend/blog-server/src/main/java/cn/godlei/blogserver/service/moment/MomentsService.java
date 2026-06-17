package cn.godlei.blogserver.service.moment;

import cn.godlei.blogpojo.dto.request.MomentQueryParam;
import cn.godlei.blogpojo.dto.request.MomentSaveBody;
import cn.godlei.blogpojo.dto.response.MomentAdminItem;
import cn.godlei.blogpojo.dto.response.MomentListItem;
import cn.godlei.blogpojo.dto.response.PageResult;

public interface MomentsService {

    PageResult<MomentListItem> listPublic(MomentQueryParam queryParam);

    MomentListItem getPublic(Long id);

    PageResult<MomentAdminItem> listAdmin(MomentQueryParam queryParam);

    void add(MomentSaveBody body);

    void update(MomentSaveBody body);

    void delete(Long id);
}
