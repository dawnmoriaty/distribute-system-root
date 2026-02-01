package com.Laliga.Demo_final_project.controller;

import com.Laliga.Demo_final_project.dto.request.CauThuRequest;
import com.Laliga.Demo_final_project.dto.response.CauThuResponse;
import com.Laliga.Demo_final_project.service.CauThuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cauthu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CauThuController {

    private final CauThuService cauThuService;

    @GetMapping
    public ResponseEntity<List<CauThuResponse>> getAllCauThu() {
        return ResponseEntity.ok(cauThuService.getAllCauThu());
    }

    @PostMapping
    public ResponseEntity<CauThuResponse> postCauThu(@RequestBody CauThuRequest request) {
        return ResponseEntity.ok(cauThuService.NhapCauThu(request));
    }

    @DeleteMapping("/{maCauThu}")
    public ResponseEntity<Void> CauThuDelete(@PathVariable String maCauThu) {
        cauThuService.CauThuDelete(maCauThu);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{maCauThu}")
    public ResponseEntity<CauThuResponse> CauThuUpdate(@PathVariable String maCauThu, @RequestBody CauThuRequest request) {
        cauThuService.CauThuUpdate(maCauThu, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{maCauThu}")
    public ResponseEntity<CauThuResponse> GetCauThu(@PathVariable String maCauThu) {
        return ResponseEntity.ok(cauThuService.getCauThu(maCauThu));
    }
}
