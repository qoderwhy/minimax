package com.qkit.system.service;

import com.qkit.common.api.R;
import com.qkit.system.domain.dto.PostQueryDTO;
import com.qkit.system.domain.dto.PostSaveDTO;
import com.qkit.system.domain.vo.PostVO;

import java.util.List;

public interface PostService {

    R<List<PostVO>> page(PostQueryDTO query);

    R<List<PostVO>> list();

    R<Long> create(PostSaveDTO dto);

    R<Boolean> update(PostSaveDTO dto);

    R<Boolean> delete(List<Long> ids);
}
