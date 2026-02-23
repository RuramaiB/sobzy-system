package com.example.sobzybackend.service;

import com.example.sobzybackend.dtos.CategoryRequest;
import com.example.sobzybackend.dtos.CategoryResponse;
import com.example.sobzybackend.exceptions.ResourceNotFoundException;
import com.example.sobzybackend.exceptions.UserAlreadyExistsException;
import com.example.sobzybackend.models.Category;
import com.example.sobzybackend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new UserAlreadyExistsException("category", request.getName());
        }

        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isAllowed(request.getIsAllowed())
                .colorCode(request.getColorCode())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created: {}", category.getId());

        return convertToResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return convertToResponse(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "name", name));
        return convertToResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllowedCategories() {
        return categoryRepository.findAllowedCategories().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getBlockedCategories() {
        return categoryRepository.findBlockedCategories().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIsAllowed() != null) {
            category.setIsAllowed(request.getIsAllowed());
        }
        if (request.getColorCode() != null) {
            category.setColorCode(request.getColorCode());
        }

        category = categoryRepository.save(category);
        log.info("Category updated: {}", id);

        return convertToResponse(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", "id", id);
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted: {}", id);
    }

    private CategoryResponse convertToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .isAllowed(category.getIsAllowed())
                .colorCode(category.getColorCode())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
