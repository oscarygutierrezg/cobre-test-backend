package co.cobre.cbmm.accounts.adapters.out.persistence.repository;

import co.cobre.cbmm.accounts.adapters.out.persistence.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Account entity
 */
@Repository
public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {

    /**
     * Find account by account number
     * @param accountNumber the account number
     * @return optional account entity
     */
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
}

