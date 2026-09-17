package com.tamar.computerstore.dto;

import com.tamar.computerstore.entity.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(example = "{\"status\":\"COMPLETED\"}")
public record UpdateOrderStatusRequest(

        @NotNull
        @Schema(description = "Only COMPLETED or CANCELLED from PENDING", example = "COMPLETED")
        OrderStatus status
) {
}
