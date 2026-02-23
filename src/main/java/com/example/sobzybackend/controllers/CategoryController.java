package com.example.sobzybackend.controllers;
import com.example.sobzybackend.dtos.CategoryRequest;
import com.example.sobzybackend.dtos.CategoryResponse;
import com.example.sobzybackend.dtos.MessageResponse;
import com.example.sobzybackend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        log.info("Create category request: {}", request.getName());
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        log.info("Get all categories");
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        log.info("Get category by ID: {}", id);
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<CategoryResponse> getCategoryByName(@PathVariable String name) {
        log.info("Get category by name: {}", name);
        CategoryResponse response = categoryService.getCategoryByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/allowed")
    public ResponseEntity<List<CategoryResponse>> getAllowedCategories() {
        log.info("Get allowed categories");
        List<CategoryResponse> categories = categoryService.getAllowedCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<CategoryResponse>> getBlockedCategories() {
        log.info("Get blocked categories");
        List<CategoryResponse> categories = categoryService.getBlockedCategories();
        return ResponseEntity.ok(categories);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        log.info("Update category: {}", id);
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteCategory(@PathVariable Long id) {
        log.info("Delete category: {}", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(MessageResponse.success("Category deleted successfully"));
    }
}