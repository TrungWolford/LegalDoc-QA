package com.contractcompiler.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link ContractVersion} entities.
 */
@Repository
public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {

    List<ContractVersion> findByContractIdOrderByVersionNoAsc(Long contractId);

    int countByContractId(Long contractId);
}
