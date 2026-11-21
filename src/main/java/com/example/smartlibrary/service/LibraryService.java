package com.example.smartlibrary.service;

import com.example.smartlibrary.dto.*;
import com.example.smartlibrary.exception.BusinessRuleException;
import com.example.smartlibrary.exception.NotFoundException;
import com.example.smartlibrary.model.*;
import com.example.smartlibrary.repository.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryService {
    private final BookRepository books;
    private final ReaderRepository readers;
    private final LoanRepository loans;
    private final Clock clock;

    public LibraryService(BookRepository books, ReaderRepository readers,
                          LoanRepository loans, Clock clock) {
        this.books = books;
        this.readers = readers;
        this.loans = loans;
        this.clock = clock;
    }

    @Transactional
    public ReaderResponse createReader(CreateReaderRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (readers.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("Reader email is already registered");
        }
        Reader reader = new Reader(request.firstName().trim(), request.lastName().trim(), email);
        return ReaderResponse.from(readers.saveAndFlush(reader));
    }

    @Transactional
    public BookResponse createBook(CreateBookRequest request) {
        String isbn = request.isbn() == null || request.isbn().isBlank() ? null : request.isbn().trim();
        Book book = new Book(request.title().trim(), request.author().trim(), isbn);
        return BookResponse.from(books.saveAndFlush(book));
    }

    @Transactional(readOnly = true)
    public List<BookResponse> getBooks() {
        return books.findAll().stream().map(BookResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReliabilityResponse getReliability(Long readerId) {
        ensureReaderExists(readerId);
        return calculateReliability(readerId, LocalDateTime.now(clock));
    }

    private ReliabilityResponse calculateReliability(Long readerId, LocalDateTime now) {
        List<Loan> returned = loans.findReturnedInPeriod(readerId, now.minusDays(30), now);
        boolean allOnTime = returned.stream().allMatch(l -> !l.getReturnedAt().isAfter(l.getDueAt()));
        boolean overdue = loans.existsByReaderIdAndReturnedAtIsNullAndDueAtBefore(readerId, now);
        long active = loans.countByReaderIdAndReturnedAtIsNull(readerId);
        boolean reliable = returned.size() >= 3 && allOnTime && !overdue;
        return new ReliabilityResponse(readerId, reliable, returned.size(), allOnTime,
                overdue, active, reliable ? 5 : 2);
    }

    @Transactional
    public LoanResponse borrow(CreateLoanRequest request) {
        // Lock the reader before checking the loan limit, then lock the book.
        // This prevents concurrent requests from bypassing the limit / borrowing one copy twice.
        Reader reader = readers.lockById(request.readerId())
                .orElseThrow(() -> new NotFoundException("Reader not found: " + request.readerId()));
        Book book = books.lockById(request.bookId())
                .orElseThrow(() -> new NotFoundException("Book not found: " + request.bookId()));
        if (book.getStatus() != BookStatus.AVAILABLE) {
            throw new BusinessRuleException("Book is already borrowed");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        ReliabilityResponse reliability = calculateReliability(reader.getId(), now);
        if (reliability.activeLoans() >= reliability.maxActiveLoans()) {
            throw new BusinessRuleException("Active loan limit reached: " + reliability.maxActiveLoans());
        }
        book.setStatus(BookStatus.BORROWED);
        Loan loan = loans.save(new Loan(reader, book, now));
        return LoanResponse.from(loan);
    }

    @Transactional
    public LoanResponse returnBook(Long loanId) {
        Loan loan = loans.lockById(loanId)
                .orElseThrow(() -> new NotFoundException("Loan not found: " + loanId));
        // Lock the book to coordinate concurrent issue/return requests.
        Book book = books.lockById(loan.getBook().getId())
                .orElseThrow(() -> new NotFoundException("Book no longer exists"));
        if (!loan.isActive()) {
            throw new BusinessRuleException("Loan has already been returned");
        }
        loan.returnAt(LocalDateTime.now(clock));
        book.setStatus(BookStatus.AVAILABLE);
        return LoanResponse.from(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getActiveLoans(Long readerId) {
        ensureReaderExists(readerId);
        return loans.findActiveLoans(readerId).stream().map(LoanResponse::from).toList();
    }

    private void ensureReaderExists(Long readerId) {
        if (!readers.existsById(readerId)) {
            throw new NotFoundException("Reader not found: " + readerId);
        }
    }
}
