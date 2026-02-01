package com.Laliga.Demo_final_project.mapper;

import com.Laliga.Demo_final_project.dto.request.CauThuRequest;
import com.Laliga.Demo_final_project.dto.response.CauThuResponse;
import com.Laliga.Demo_final_project.entity.CauThu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CauThuMapper {
    @Mapping(source = "doiBong.tenDoi", target = "tenDoiBong")
    CauThuResponse toResponse(CauThu cauThu);

    @Mapping(target = "doiBong", ignore = true) // xử lý thủ công
    CauThu toCauThu(CauThuRequest request);
}
