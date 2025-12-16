package org.example.flowershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SaveProductRequest {
    private String name;
    private String description;
    private double price;
    private String image;
    private long categoryId;

}