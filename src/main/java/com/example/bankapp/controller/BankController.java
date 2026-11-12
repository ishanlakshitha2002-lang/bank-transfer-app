
package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // Display the main bank page
    @GetMapping("/")
    public String home(Model model) {

        List<Customer> customers = customerRepository.findAllOrdered();

        model.addAttribute("customers", customers);
        model.addAttribute("customer", new Customer());
        model.addAttribute("transferRequest", new TransferRequest());

        return "bank";
    }

    // Create a new customer
    @PostMapping("/customers")
    public String createCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Please enter valid customer details.");
            return "redirect:/";
        }

        try {
            bankService.createCustomer(customer);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Customer created successfully.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/";
    }

    // Transfer money between accounts
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Please enter valid transfer details.");
            return "redirect:/";
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.getDescription(),
                    request.isSimulateFailure()
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Transfer completed successfully.");

        } catch (Exception e) {

            try {
                auditService.recordFailedTransaction(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        e.getMessage()
                );
            } catch (Exception auditException) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "Transfer failed and audit record could not be saved.");
                return "redirect:/";
            }

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Transfer failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    // Display transaction history of an account
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model,
            RedirectAttributes redirectAttributes) {

        Customer customer = customerRepository
                .getByAccountNumber(accountNumber)
                .orElse(null);

        if (customer == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Account not found.");
            return "redirect:/";
        }

        List<BankTransaction> transactions =
                transactionRepository.findHistory(accountNumber);

        BigDecimal totalSent = transactions.stream()
                .filter(t -> t.getFromAccount().equals(accountNumber))
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(BankTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalSent", totalSent);

        return "history";
    }
}
