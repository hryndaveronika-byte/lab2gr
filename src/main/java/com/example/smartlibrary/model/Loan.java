package com.example.smartlibrary.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans", indexes = {
        @Index(name = "idx_loan_reader_return", columnList = "reader_id,returned_at"),
        @Index(name = "idx_loan_book_return", columnList = "book_id,returned_at")
})
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "borrowed_at", nullable = false)
    private LocalDateTime borrowedAt;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    protected Loan() { }

    public Loan(Reader reader, Book book, LocalDateTime borrowedAt) {
        this.reader = reader;
        this.book = book;
        this.borrowedAt = borrowedAt;
        this.dueAt = borrowedAt.plusDays(14);
    }

    public Long getId() { return id; }
    public Reader getReader() { return reader; }
    public Book getBook() { return book; }
    public LocalDateTime getBorrowedAt() { return borrowedAt; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public boolean isActive() { return returnedAt == null; }
    public void returnAt(LocalDateTime time) { this.returnedAt = time; }
}
