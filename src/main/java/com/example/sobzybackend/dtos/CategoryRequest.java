package com.example.sobzybackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name must not exceed 50 characters")
    private String name;

    private String description;

    private Boolean isAllowed = true;

    private String colorCode;

    public CategoryRequest() {}

    public CategoryRequest(String name, String description, Boolean isAllowed, String colorCode) {
        this.name = name;
        this.description = description;
        this.isAllowed = isAllowed;
        this.colorCode = colorCode;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsAllowed() { return isAllowed; }
    public void setIsAllowed(Boolean isAllowed) { this.isAllowed = isAllowed; }
    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public static CategoryRequestBuilder builder() {
        return new CategoryRequestBuilder();
    }

    public static class CategoryRequestBuilder {
        private String name;
        private String description;
        private Boolean isAllowed = true;
        private String colorCode;

        public CategoryRequestBuilder name(String name) { this.name = name; return this; }
        public CategoryRequestBuilder description(String description) { this.description = description; return this; }
        public CategoryRequestBuilder isAllowed(Boolean isAllowed) { this.isAllowed = isAllowed; return this; }
        public CategoryRequestBuilder colorCode(String colorCode) { this.colorCode = colorCode; return this; }

        public CategoryRequest build() {
            return new CategoryRequest(name, description, isAllowed, colorCode);
        }
    }
}
