package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.CreateLoanRequest;
import com.example.smartlibrary.dto.LoanResponse;
import com.example.smartlibrary.service.LibraryService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
public class LoanController {
    private final LibraryService service;
    public LoanController(LibraryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<LoanResponse> borrow(@Valid @RequestBody CreateLoanRequest request) {
        LoanResponse loan = service.borrow(request);
        return ResponseEntity.created(URI.create("/api/loans/" + loan.id())).body(loan);
    }

    @PostMapping("/{loanId}/return")
    public LoanResponse returnBook(@PathVariable Long loanId) { return service.returnBook(loanId); }
}
