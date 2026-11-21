package com.example.smartlibrary.repository;

import com.example.smartlibrary.model.Loan;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> lockById(@Param("id") Long id);

    long countByReaderIdAndReturnedAtIsNull(Long readerId);

    @Query("select l from Loan l join fetch l.book where l.reader.id = :readerId and l.returnedAt is null order by l.borrowedAt desc")
    List<Loan> findActiveLoans(@Param("readerId") Long readerId);

    @Query("select l from Loan l where l.reader.id = :readerId and l.returnedAt between :start and :end")
    List<Loan> findReturnedInPeriod(@Param("readerId") Long readerId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    boolean existsByReaderIdAndReturnedAtIsNullAndDueAtBefore(Long readerId, LocalDateTime now);
}
