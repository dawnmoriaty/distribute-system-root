package com.Laliga.Demo_final_project.service;

import com.Laliga.Demo_final_project.dto.request.CauThuRequest;
import com.Laliga.Demo_final_project.dto.response.CauThuResponse;
import com.Laliga.Demo_final_project.entity.CauThu;
import com.Laliga.Demo_final_project.entity.DoiBong;
import com.Laliga.Demo_final_project.mapper.CauThuMapper;
import com.Laliga.Demo_final_project.repository.CauThuRepository;
import com.Laliga.Demo_final_project.repository.DoiBongRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CauThuService {
    private final CauThuRepository cauThuRepository;
    private final DoiBongRepository doiBongRepository;
    private final CauThuMapper cauThuMapper;

    public List<CauThuResponse> getAllCauThu() {
        return cauThuRepository.findAll()
                .stream()
                .map(cauThuMapper::toResponse)
                .collect(Collectors.toList());
    }

    public CauThuResponse NhapCauThu(CauThuRequest request) {
        CauThu cauThu = cauThuMapper.toCauThu(request);
        DoiBong doiBong = doiBongRepository.findByTenDoi(request.getTenDoiBong())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng: " + request.getTenDoiBong()));
        cauThu.setDoiBong(doiBong);
        return cauThuMapper.toResponse(cauThuRepository.save(cauThu));
    }

    public void CauThuDelete(String maCauThu) {
        if (!cauThuRepository.existsById(maCauThu)) {
            throw new EntityNotFoundException("Không tìm thấy cầu thủ: " + maCauThu);
        }
        cauThuRepository.deleteById(maCauThu);
    }

    public CauThuResponse CauThuUpdate(String maCauThu, CauThuRequest request) {
        if (!cauThuRepository.existsById(maCauThu)) {
            throw new EntityNotFoundException("Không tìm thấy cầu thủ: " + maCauThu);
        }
        CauThu cauThu = cauThuMapper.toCauThu(request);
        DoiBong doiBong = doiBongRepository.findByTenDoi(request.getTenDoiBong())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đội bóng: " + request.getTenDoiBong()));
        cauThu.setDoiBong(doiBong);
        cauThuRepository.save(cauThu);
        return cauThuMapper.toResponse(cauThu);
    }

    public CauThuResponse getCauThu(String maCauThu) {
        if (!cauThuRepository.existsById(maCauThu)) {
            throw new EntityNotFoundException("Không tìm thấy cầu thủ: " + maCauThu);
        }
        return cauThuMapper.toResponse(cauThuRepository.findById(maCauThu).get());
    }
}
