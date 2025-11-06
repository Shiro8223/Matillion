package com.matillion.techtest2025.controller.response;

public record OutlierSummary(
        Double lowerFence,
        Double upperFence,
        Integer outlierCount) {
}
