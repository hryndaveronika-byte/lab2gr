package com.example.smartlibrary.dto;

public record ReliabilityResponse(Long readerId, boolean reliable,
                                  long returnedLast30Days, boolean allReturnedOnTime,
                                  boolean hasOverdueActiveLoans, long activeLoans,
                                  int maxActiveLoans) { }
