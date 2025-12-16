package org.example.flowershop.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteDto {
    private long id;
    private long userId;
    private long productId;
    private String productName;
    private double productPrice;
    private String productDescription;
    private String productImage;
}