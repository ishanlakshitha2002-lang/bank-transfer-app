
package com.example.bankapp.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6}",
             message = "Account number must contain 6 digits")
    @Column(nullable = false, unique = true, length = 6)
    private String accountNumber;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Column(nullable = false, unique = true)
    private String email;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.00",
                message = "Balance cannot be negative")
    @Digits(integer = 12, fraction = 2)
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

}