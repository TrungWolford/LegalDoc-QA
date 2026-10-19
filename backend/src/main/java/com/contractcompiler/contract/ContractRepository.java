package com.contractcompiler.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Contract} entities.
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("SELECT c FROM Contract c JOIN FETCH c.createdBy ORDER BY c.createdAt DESC")
    List<Contract> findAllWithCreator();
}
