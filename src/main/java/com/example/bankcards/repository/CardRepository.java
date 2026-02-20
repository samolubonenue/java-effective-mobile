package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

    Page<Card> findByOwner(User owner, Pageable pageable);

    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

    List<Card> findByOwner(User owner);

    Optional<Card> findByIdAndOwner(Long id, User owner);

    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId")
    Page<Card> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT c FROM Card c WHERE c.owner.id = :ownerId AND c.status = :status")
    Page<Card> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId,
                                       @Param("status") CardStatus status,
                                       Pageable pageable);

    boolean existsByIdAndOwnerId(Long id, Long ownerId);
}
