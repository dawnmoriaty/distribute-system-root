package com.Laliga.Demo_final_project.service;

import com.Laliga.Demo_final_project.dto.request.DoiBongRequest;
import com.Laliga.Demo_final_project.dto.response.DoiBongResponse;
import com.Laliga.Demo_final_project.entity.DoiBong;
import com.Laliga.Demo_final_project.entity.SanThiDau;
import com.Laliga.Demo_final_project.mapper.DoiBongMapper;
import com.Laliga.Demo_final_project.repository.DoiBongRepository;
import com.Laliga.Demo_final_project.repository.SanThiDauRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoiBongService {

    private final DoiBongRepository doiBongRepository;
    private final SanThiDauRepository sanThiDauRepository;
    private final DoiBongMapper doiBongMapper;

    public List<DoiBongResponse> getAllDoiBong() {
        return doiBongRepository.findAll()
                .stream()
                .map(doiBongMapper::toResponse)
                .collect(Collectors.toList());
    }

    public DoiBongResponse DoiBongUpdate (Integer maDoi, DoiBongRequest request) {
        if (!doiBongRepository.existsById(maDoi)) {
            throw new EntityNotFoundException("Không tìm thấy đội bóng: " + maDoi);
        }
        DoiBong doiBong = doiBongMapper.toDoiBong(request);
        SanThiDau sanThiDau = sanThiDauRepository.findByTenSon(request.getTenSanNha())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sân: " + request.getTenSanNha()));
        doiBong.setSanNha(sanThiDau);
        doiBongRepository.save(doiBong);
        return doiBongMapper.toResponse(doiBong);
    }
}
