package org.example.flowershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartDto {
    private long id;
    private long productId;
    private String productName;
    private String productDescription;
    private double productPrice;
    private String productImage;

}