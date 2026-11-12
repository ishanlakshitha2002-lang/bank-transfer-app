
package com.example.bankapp.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Sender account number is required")
    @Pattern(regexp = "\\d{6}", message = "Account number must contain 6 digits")
    private String fromAccount;

    @NotBlank(message = "Receiver account number is required")
    @Pattern(regexp = "\\d{6}", message = "Account number must contain 6 digits")
    private String toAccount;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private boolean simulateFailure;
}
