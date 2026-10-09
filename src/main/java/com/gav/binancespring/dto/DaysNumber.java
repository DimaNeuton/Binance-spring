package com.gav.binancespring.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DaysNumber {

    @NotNull
    @Min(value = 1, message = "Количество дней должно быть не менее 1")
    private int number = 1;
}
