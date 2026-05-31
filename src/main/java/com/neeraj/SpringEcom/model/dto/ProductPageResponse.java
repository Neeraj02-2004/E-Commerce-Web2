package com.neeraj.SpringEcom.model.dto;

import com.neeraj.SpringEcom.model.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductPageResponse {

    private List<Product> content;
    private int page;
    private int size;
    private long totalElements;
}
