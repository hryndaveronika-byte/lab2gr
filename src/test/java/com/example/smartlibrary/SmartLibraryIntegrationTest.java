package com.example.smartlibrary;

import com.example.smartlibrary.dto.*;
import com.example.smartlibrary.model.Loan;
import com.example.smartlibrary.repository.*;
import com.example.smartlibrary.service.LibraryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SmartLibraryIntegrationTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 12, 0);

    @TestConfiguration
    static class FixedClockConfig {
        @Bean @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired private LibraryService library;
    @Autowired private ReaderRepository readers;
    @Autowired private BookRepository books;
    @Autowired private LoanRepository loans;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;

    @BeforeEach
    void clean() {
        loans.deleteAll();
        books.deleteAll();
        readers.deleteAll();
    }

    @Test
    void newReaderIsOrdinaryAndLimitedToTwoBooks() {
        var reader = library.createReader(new CreateReaderRequest("Anna", "Ivanenko", "Anna@Example.com"));
        assertEquals("anna@example.com", reader.email());
        var b1 = library.createBook(new CreateBookRequest("Book 1", "Author", null));
        var b2 = library.createBook(new CreateBookRequest("Book 2", "Author", null));
        var b3 = library.createBook(new CreateBookRequest("Book 3", "Author", null));
        assertFalse(library.getReliability(reader.id()).reliable());
        library.borrow(new CreateLoanRequest(reader.id(), b1.id()));
        library.borrow(new CreateLoanRequest(reader.id(), b2.id()));
        assertEquals(2, library.getActiveLoans(reader.id()).size());
        assertThrows(RuntimeException.class,
                () -> library.borrow(new CreateLoanRequest(reader.id(), b3.id())));
    }

    @Test
    void threeOnTimeReturnsMakeReaderReliableAndAllowFiveBooks() {
        var reader = library.createReader(new CreateReaderRequest("Oleh", "Petrenko", "oleh@example.com"));
        for (int i = 0; i < 3; i++) {
            var book = library.createBook(new CreateBookRequest("Book " + i, "Author", null));
            var loan = library.borrow(new CreateLoanRequest(reader.id(), book.id()));
            library.returnBook(loan.id());
        }
        var status = library.getReliability(reader.id());
        assertTrue(status.reliable());
        assertEquals(5, status.maxActiveLoans());
        assertEquals(3, status.returnedLast30Days());
        for (int i = 0; i < 5; i++) {
            var book = library.createBook(new CreateBookRequest("Extra " + i, "Author", null));
            library.borrow(new CreateLoanRequest(reader.id(), book.id()));
        }
        assertEquals(5, library.getActiveLoans(reader.id()).size());
    }

    @Test
    void overdueReturnedBookDisqualifiesReader() {
        var reader = library.createReader(new CreateReaderRequest("Test", "Reader", "test@example.com"));
        for (int i = 0; i < 3; i++) {
            var book = books.save(new com.example.smartlibrary.model.Book("Old " + i, "Author", null));
            Loan oldLoan = new Loan(readers.findById(reader.id()).orElseThrow(), book, NOW.minusDays(i == 0 ? 20 : 5));
            oldLoan.returnAt(NOW.minusDays(1));
            loans.save(oldLoan);
        }
        var status = library.getReliability(reader.id());
        assertFalse(status.reliable());
        assertFalse(status.allReturnedOnTime());
        assertEquals(2, status.maxActiveLoans());
    }

    @Test
    void returnUpdatesBookAndKeepsLoanHistory() {
        var reader = library.createReader(new CreateReaderRequest("A", "B", "a@b.com"));
        var book = library.createBook(new CreateBookRequest("Clean Code", "Robert Martin", null));
        var loan = library.borrow(new CreateLoanRequest(reader.id(), book.id()));
        assertEquals(14, java.time.Duration.between(loan.borrowedAt(), loan.dueAt()).toDays());
        assertThrows(RuntimeException.class, () -> library.borrow(new CreateLoanRequest(reader.id(), book.id())));
        var returned = library.returnBook(loan.id());
        assertFalse(returned.active());
        assertNotNull(returned.returnedAt());
        assertTrue(library.getActiveLoans(reader.id()).isEmpty());
        assertEquals(1, loans.count());
        assertEquals(com.example.smartlibrary.model.BookStatus.AVAILABLE,
                books.findById(book.id()).orElseThrow().getStatus());
        assertThrows(RuntimeException.class, () -> library.returnBook(loan.id()));
    }

    @Test
    void apiValidatesEmailAndReturnsHttpCodes() throws Exception {
        String json = mapper.writeValueAsString(new CreateReaderRequest("Anna", "Ivanenko", "anna@example.com"));
        mvc.perform(post("/api/readers").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("anna@example.com"));
        mvc.perform(post("/api/readers").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/readers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"email\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/readers/999999/reliability")).andExpect(status().isNotFound());
    }
}
