package com.Laliga.Demo_final_project.mapper;

import com.Laliga.Demo_final_project.dto.request.DoiBongRequest;
import com.Laliga.Demo_final_project.dto.response.DoiBongResponse;
import com.Laliga.Demo_final_project.entity.DoiBong;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoiBongMapper {

    @Mapping(source = "sanNha.tenSon", target = "tenSanNha")
    DoiBongResponse toResponse(DoiBong entity);

    @Mapping(target = "sanNha", ignore = true)
    DoiBong toDoiBong(DoiBongRequest request);
}
