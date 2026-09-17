package com.tamar.computerstore.persistence;

import com.tamar.computerstore.entity.Customer;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class CustomerRepositoryIntegrationTests extends AbstractMySqlIntegrationTests {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void savesAndReloadsCustomerWithUserAccount() {
        UserAccount account = userAccountRepository.saveAndFlush(
                new UserAccount("dana@example.com", "not-a-real-hash", UserRole.CUSTOMER)
        );
        Customer customer = new Customer(account, "Dana", "Levi");
        customer.setPhone("050-1234567");
        customer.setAddress("12 Herzl St, Tel Aviv");
        customerRepository.saveAndFlush(customer);

        Optional<Customer> reloaded = customerRepository.findByUserAccountEmail("dana@example.com");

        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.isEnabled()).isTrue();
        assertThat(reloaded).isPresent().get().satisfies(found -> {
            assertThat(found.getFirstName()).isEqualTo("Dana");
            assertThat(found.getLastName()).isEqualTo("Levi");
            assertThat(found.getPhone()).isEqualTo("050-1234567");
            assertThat(found.getUserAccount().getId()).isEqualTo(account.getId());
            assertThat(found.getUserAccount().getRole()).isEqualTo(UserRole.CUSTOMER);
        });
    }

    @Test
    void rejectsDuplicateEmail() {
        userAccountRepository.saveAndFlush(new UserAccount("taken@example.com", "not-a-real-hash", UserRole.CUSTOMER));

        assertThatThrownBy(() -> userAccountRepository.saveAndFlush(
                new UserAccount("taken@example.com", "another-hash", UserRole.ADMIN)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void adminAccountNeedsNoCustomerProfile() {
        UserAccount admin = userAccountRepository.saveAndFlush(
                new UserAccount("admin@example.com", "not-a-real-hash", UserRole.ADMIN)
        );

        assertThat(admin.getId()).isNotNull();
        assertThat(customerRepository.findByUserAccountId(admin.getId())).isEmpty();
    }
}
