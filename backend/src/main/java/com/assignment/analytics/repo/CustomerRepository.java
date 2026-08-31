package com.assignment.analytics.repo;

import com.assignment.analytics.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("""
            SELECT c FROM Customer c
            WHERE lower(c.fullName) LIKE lower(concat('%', :query, '%'))
               OR cast(c.customerId as string) LIKE lower(concat('%', :query, '%'))
            ORDER BY c.fullName
            """)
    List<Customer> search(@Param("query") String query);
}
