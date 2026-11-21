package com.example.smartlibrary.controller;

import com.example.smartlibrary.dto.BookResponse;
import com.example.smartlibrary.dto.CreateBookRequest;
import com.example.smartlibrary.service.LibraryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final LibraryService service;
    public BookController(LibraryService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        BookResponse book = service.createBook(request);
        return ResponseEntity.created(URI.create("/api/books/" + book.id())).body(book);
    }

    @GetMapping
    public List<BookResponse> all() { return service.getBooks(); }
}
