package com.example.bankcards;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardJpaRepository;
import com.example.bankcards.repository.UserJpaRepository;
import com.example.bankcards.security.CardNumberHasher;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Locking is where the database engine matters most: row locks and version
 * checks depend on the real transaction implementation, so these run against
 * PostgreSQL. Deliberately not transactional — the work must actually commit
 * for a race to exist — so each test cleans up after itself.
 */
class PostgresLockingIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private CardJpaRepository cardJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private CardNumberHasher cardNumberHasher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final BigDecimal STARTING_BALANCE = new BigDecimal("100.00");
    private static final String USERNAME = "pg-locking-user";

    private String fromId;
    private String toId;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(USERNAME);
        user.setPassword("irrelevant-for-this-test");
        user.setRole(Role.USER);
        userJpaRepository.save(user);

        fromId = saveCard("4111111111111111", user.getId(), STARTING_BALANCE);
        toId = saveCard("4222222222222222", user.getId(), BigDecimal.ZERO);

        auth = new UsernamePasswordAuthenticationToken(
                USERNAME, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @AfterEach
    void tearDown() {
        cardJpaRepository.deleteAllById(List.of(fromId, toId));
        userJpaRepository.findByUsername(USERNAME).ifPresent(userJpaRepository::delete);
    }

    private String saveCard(String number, String holderId, BigDecimal balance) {
        Card card = new Card();
        card.setId(UUID.randomUUID().toString());
        card.setNumber(number);
        card.setNumberHash(cardNumberHasher.hash(number));
        card.setLast4(number.substring(number.length() - 4));
        card.setHolderId(holderId);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(balance);
        card.setExpirationDate(LocalDate.now().plusYears(1));
        return cardJpaRepository.save(card).getId();
    }

    /**
     * Deterministic proof that the transfer path really takes a row lock:
     * while one transaction holds it, a second one cannot acquire it and hits
     * the lock timeout instead of reading a balance that is about to change.
     */
    @Test
    void rowLockBlocksAnotherTransactionOnTheSameCard() throws Exception {
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService holder = Executors.newSingleThreadExecutor();

        Future<?> holding = holder.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            cardJpaRepository.findByIdForUpdate(fromId).orElseThrow();
            locked.countDown();
            try {
                release.await(20, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        assertThat(locked.await(20, TimeUnit.SECONDS)).isTrue();

        try {
            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                jdbcTemplate.execute("SET LOCAL lock_timeout = '1s'");
                cardJpaRepository.findByIdForUpdate(fromId);
            })).isInstanceOf(DataAccessException.class);
        } finally {
            release.countDown();
            holding.get(20, TimeUnit.SECONDS);
            holder.shutdownNow();
        }
    }

    /**
     * End-to-end invariant check. Thread scheduling decides whether the two
     * transfers actually overlap, so this guards the outcome rather than any
     * particular mechanism; the two tests above are the deterministic ones.
     */
    @Test
    void concurrentTransfersOfTheFullBalanceCannotOverdrawTheCard() throws Exception {
        int threads = 2;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger succeeded = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    cardService.transfer(fromId, toId, STARTING_BALANCE, auth);
                    succeeded.incrementAndGet();
                } catch (Exception expectedForTheLoser) {
                    // Losing the race must fail the transfer, not overdraw.
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        BigDecimal fromBalance = cardJpaRepository.findById(fromId).orElseThrow().getBalance();
        BigDecimal toBalance = cardJpaRepository.findById(toId).orElseThrow().getBalance();

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(fromBalance).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(toBalance).isEqualByComparingTo(STARTING_BALANCE);
        assertThat(fromBalance.add(toBalance)).isEqualByComparingTo(STARTING_BALANCE);
    }

    @Test
    void staleWriteIsRejectedByOptimisticLocking() {
        Card stale = cardJpaRepository.findById(fromId).orElseThrow();
        Long staleVersion = stale.getVersion();

        // Someone else updates the same row and commits, bumping the version.
        transactionTemplate.executeWithoutResult(status -> {
            Card fresh = cardJpaRepository.findById(fromId).orElseThrow();
            fresh.setBalance(new BigDecimal("55.00"));
            cardJpaRepository.saveAndFlush(fresh);
        });

        stale.setBalance(new BigDecimal("999.00"));

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
                status -> cardJpaRepository.saveAndFlush(stale)
        )).isInstanceOf(OptimisticLockingFailureException.class);

        Card persisted = cardJpaRepository.findById(fromId).orElseThrow();
        assertThat(persisted.getBalance()).isEqualByComparingTo("55.00");
        assertThat(persisted.getVersion()).isGreaterThan(staleVersion);
    }
}
