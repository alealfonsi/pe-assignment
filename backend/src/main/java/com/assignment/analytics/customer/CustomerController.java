package com.assignment.analytics.customer;

import com.assignment.analytics.customer.CustomerDtos.CustomerOverview;
import com.assignment.analytics.customer.CustomerDtos.CustomerSearchItem;
import com.assignment.analytics.customer.CustomerDtos.TransactionsPage;
import com.assignment.analytics.domain.ActivityType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public List<CustomerSearchItem> search(@RequestParam(required = false) String query) {
        return customerService.search(query);
    }

    @GetMapping("/{customerId}")
    public CustomerOverview overview(@PathVariable UUID customerId) {
        return customerService.overview(customerId);
    }

    @GetMapping("/{customerId}/transactions")
    public TransactionsPage transactions(@PathVariable UUID customerId,
                                         @RequestParam(required = false) ActivityType type,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return customerService.transactionsPage(customerId, type, page, Math.min(size, 100));
    }
}
