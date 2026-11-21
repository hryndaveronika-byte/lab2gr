package com.example.smartlibrary.dto;

import com.example.smartlibrary.model.Loan;
import java.time.LocalDateTime;

public record LoanResponse(Long id, Long readerId, Long bookId, String bookTitle,
                           LocalDateTime borrowedAt, LocalDateTime dueAt,
                           LocalDateTime returnedAt, boolean active) {
    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getReader().getId(), loan.getBook().getId(),
                loan.getBook().getTitle(), loan.getBorrowedAt(), loan.getDueAt(),
                loan.getReturnedAt(), loan.isActive());
    }
}
