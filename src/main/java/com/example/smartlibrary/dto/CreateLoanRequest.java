package com.example.smartlibrary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLoanRequest(
        @NotNull @Positive Long readerId,
        @NotNull @Positive Long bookId
) { }
