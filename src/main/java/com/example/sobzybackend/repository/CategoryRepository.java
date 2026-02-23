package com.example.sobzybackend.repository;


import com.example.sobzybackend.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    List<Category> findByIsAllowed(Boolean isAllowed);

    @Query("SELECT c FROM Category c WHERE c.isAllowed = true")
    List<Category> findAllowedCategories();

    @Query("SELECT c FROM Category c WHERE c.isAllowed = false")
    List<Category> findBlockedCategories();

    boolean existsByName(String name);
}
