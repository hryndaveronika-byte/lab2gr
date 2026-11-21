package com.example.smartlibrary.dto;

import com.example.smartlibrary.model.Book;
import com.example.smartlibrary.model.BookStatus;

public record BookResponse(Long id, String title, String author, String isbn, BookStatus status) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getIsbn(), book.getStatus());
    }
}
