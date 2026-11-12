
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

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;
    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // Display the main bank page
    @GetMapping("/")
    public String home(
            @RequestParam(required = false) String keyword,
            Model model) {

        List<Customer> customers;

        if (keyword != null && !keyword.isBlank()) {
            customers = customerRepository.searchByName(keyword);
        } else {
            customers = customerRepository.findAllOrdered();
        }

        model.addAttribute("customers", customers);
        model.addAttribute("allCustomers",
                customerRepository.findAllOrdered());

        model.addAttribute("customer", new Customer());
        model.addAttribute("transferRequest", new TransferRequest());

        model.addAttribute("totalCustomers",
                customerRepository.countAccounts());

        model.addAttribute("totalBalance",
                customerRepository.getTotalBalance());

        model.addAttribute("successCount",
                transactionRepository.countByStatus(
                        TransactionStatus.SUCCESS));

        model.addAttribute("failedCount",
                transactionRepository.countByStatus(
                        TransactionStatus.FAILED));

        model.addAttribute("transactions",
                transactionRepository.findAllLatest());

        model.addAttribute("keyword", keyword);

        return "bank";
    }

    // Create a new customer using web form
    @PostMapping("/customers")
    public String createCustomer(
            @Valid @ModelAttribute("customer") Customer customer,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid customer details.");

            return "redirect:/";
        }

        try {
            bankService.createCustomer(customer);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Customer created successfully.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage());
        }

        return "redirect:/";
    }

    // Create a new customer using REST API (Postman)
    @PostMapping("/api/customers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createCustomerApi(
            @Valid @RequestBody Customer customer,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "FAILED",
                            "message", result.getFieldErrors()
                                    .get(0).getDefaultMessage()
                    )
            );
        }

        try {
            bankService.createCustomer(customer);

            return ResponseEntity.ok(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "Customer created successfully."
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "FAILED",
                            "message", String.valueOf(e.getMessage())
                    )
            );
        }
    }

    // Transfer money using web form
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter valid transfer details.");

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
                    "successMessage",
                    "Transfer completed successfully.");

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
                        "errorMessage",
                        "Transfer failed and audit record could not be saved.");

                return "redirect:/";
            }

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Transfer failed: " + e.getMessage());
        }

        return "redirect:/";
    }

    // Transfer money using REST API (Postman)
    @PostMapping("/api/transfer")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> transferApi(
            @Valid @RequestBody TransferRequest request,
            BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "FAILED",
                            "message", result.getFieldErrors()
                                    .get(0).getDefaultMessage()
                    )
            );
        }

        try {
            bankService.transfer(
                    request.getFromAccount(),
                    request.getToAccount(),
                    request.getAmount(),
                    request.getDescription(),
                    request.isSimulateFailure()
            );

            return ResponseEntity.ok(
                    Map.of(
                            "status", "SUCCESS",
                            "message", "Transfer completed successfully."
                    )
            );

        } catch (Exception e) {

            try {
                auditService.recordFailedTransaction(
                        request.getFromAccount(),
                        request.getToAccount(),
                        request.getAmount(),
                        e.getMessage()
                );

            } catch (Exception auditException) {
                return ResponseEntity.internalServerError().body(
                        Map.of(
                                "status", "FAILED",
                                "message",
                                "Transfer failed and audit record could not be saved."
                        )
                );
            }

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "status", "FAILED",
                            "message", e.getMessage()
                    )
            );
        }
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
                    "errorMessage",
                    "Account not found.");

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
