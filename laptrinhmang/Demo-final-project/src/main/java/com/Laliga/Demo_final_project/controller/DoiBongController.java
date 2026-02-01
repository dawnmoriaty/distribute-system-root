package com.Laliga.Demo_final_project.controller;

import com.Laliga.Demo_final_project.dto.request.DoiBongRequest;
import com.Laliga.Demo_final_project.dto.response.DoiBongResponse;
import com.Laliga.Demo_final_project.service.DoiBongService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doibong")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // CORS nếu frontend chạy riêng biệt
public class DoiBongController {

    private final DoiBongService doiBongService;

    @GetMapping
    public ResponseEntity<List<DoiBongResponse>> getAllDoiBong() {
        return ResponseEntity.ok(doiBongService.getAllDoiBong());
    }

    @PutMapping("/{MaDoiBong}")
    public ResponseEntity<DoiBongResponse> DoiBongUpdate(@PathVariable Integer MaDoiBong, @RequestBody DoiBongRequest request) {
        doiBongService.DoiBongUpdate(MaDoiBong, request);
        return ResponseEntity.noContent().build();
    }
}
