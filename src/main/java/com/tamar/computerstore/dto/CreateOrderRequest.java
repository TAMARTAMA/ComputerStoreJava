package com.tamar.computerstore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(example = "{\"items\":[{\"productId\":1,\"quantity\":2}]}")
public record CreateOrderRequest(

        @NotEmpty
        List<@Valid OrderLineRequest> items
) {
}
