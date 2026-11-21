package com.example.smartlibrary.repository;

import com.example.smartlibrary.model.Reader;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReaderRepository extends JpaRepository<Reader, Long> {
    boolean existsByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reader r where r.id = :id")
    Optional<Reader> lockById(@Param("id") Long id);
}
