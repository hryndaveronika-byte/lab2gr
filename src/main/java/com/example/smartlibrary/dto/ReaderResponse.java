package com.example.smartlibrary.dto;

import com.example.smartlibrary.model.Reader;

public record ReaderResponse(Long id, String firstName, String lastName, String email) {
    public static ReaderResponse from(Reader reader) {
        return new ReaderResponse(reader.getId(), reader.getFirstName(), reader.getLastName(), reader.getEmail());
    }
}
