package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.*;
import com.example.smartlibrary.service.LibraryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/readers")
public class ReaderController {
    private final LibraryService service;
    public ReaderController(LibraryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ReaderResponse> create(@Valid @RequestBody CreateReaderRequest request) {
        ReaderResponse reader = service.createReader(request);
        return ResponseEntity.created(URI.create("/api/readers/" + reader.id())).body(reader);
    }

    @GetMapping("/{readerId}/reliability")
    public ReliabilityResponse reliability(@PathVariable Long readerId) {
        return service.getReliability(readerId);
    }

    @GetMapping("/{readerId}/loans/active")
    public List<LoanResponse> activeLoans(@PathVariable Long readerId) {
        return service.getActiveLoans(readerId);
    }
}
