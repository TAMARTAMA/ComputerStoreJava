package com.tamar.computerstore.service;

import com.tamar.computerstore.dto.AuthResponse;
import com.tamar.computerstore.dto.AuthenticatedUserSummary;
import com.tamar.computerstore.dto.CurrentUserResponse;
import com.tamar.computerstore.dto.LoginRequest;
import com.tamar.computerstore.dto.RegisterRequest;
import com.tamar.computerstore.entity.Customer;
import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import com.tamar.computerstore.exception.DuplicateEmailException;
import com.tamar.computerstore.exception.InvalidCredentialsException;
import com.tamar.computerstore.repository.CustomerRepository;
import com.tamar.computerstore.repository.UserAccountRepository;
import com.tamar.computerstore.security.AuthenticatedUser;
import com.tamar.computerstore.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Compared against when no account matches, so a failed login costs the same whether or
     * not the email exists and cannot be timed to enumerate accounts.
     */
    private final String decoyPasswordHash;

    public AuthService(UserAccountRepository userAccountRepository,
                       CustomerRepository customerRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.decoyPasswordHash = passwordEncoder.encode("no-account-matches-this-password");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userAccountRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        UserAccount account = new UserAccount(email, passwordEncoder.encode(request.password()), UserRole.CUSTOMER);
        Customer customer = new Customer(account, request.firstName().trim(), request.lastName().trim());
        customer.setPhone(trimToNull(request.phone()));
        customer.setAddress(trimToNull(request.address()));

        try {
            userAccountRepository.saveAndFlush(account);
            customerRepository.saveAndFlush(customer);
        } catch (DataIntegrityViolationException exception) {
            // Two concurrent registrations for the same address; the unique key decides the winner.
            throw new DuplicateEmailException(email);
        }

        return toAuthResponse(account, customer.getFirstName(), customer.getLastName());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Optional<UserAccount> account = userAccountRepository.findByEmail(email);

        if (account.isEmpty()) {
            passwordEncoder.matches(request.password(), decoyPasswordHash);
            throw new InvalidCredentialsException();
        }

        UserAccount user = account.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()) || !user.isEnabled()) {
            throw new InvalidCredentialsException();
        }

        Optional<Customer> profile = customerRepository.findByUserAccountId(user.getId());
        return toAuthResponse(
                user,
                profile.map(Customer::getFirstName).orElse(null),
                profile.map(Customer::getLastName).orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUser(AuthenticatedUser principal) {
        UserAccount account = userAccountRepository.findById(principal.getId())
                .orElseThrow(InvalidCredentialsException::new);
        Optional<Customer> profile = customerRepository.findByUserAccountId(account.getId());

        return new CurrentUserResponse(
                account.getId(),
                account.getEmail(),
                account.getRole(),
                account.isEnabled(),
                profile.map(Customer::getFirstName).orElse(null),
                profile.map(Customer::getLastName).orElse(null),
                profile.map(Customer::getPhone).orElse(null),
                profile.map(Customer::getAddress).orElse(null)
        );
    }

    private AuthResponse toAuthResponse(UserAccount account, String firstName, String lastName) {
        AuthenticatedUserSummary summary = new AuthenticatedUserSummary(
                account.getId(), account.getEmail(), account.getRole(), firstName, lastName
        );
        return AuthResponse.bearer(jwtService.generateToken(account), jwtService.expiresInSeconds(), summary);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
