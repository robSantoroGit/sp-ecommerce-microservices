package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    /**
     * Convert Order entity to OrderResponseDTO
     */
    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                .map(this::toItemResponseDTO)
                .collect(Collectors.toList());

        return new OrderResponseDTO(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getOrderDate(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                itemDTOs
        );
    }

    /**
     * Convert OrderItem entity to OrderItemResponseDTO (without product name enrichment)
     */
    public OrderItemResponseDTO toItemResponseDTO(OrderItem item) {
        if (item == null) {
            return null;
        }

        return new OrderItemResponseDTO(
                item.getId(),
                item.getProductId(),
                null,  // productName will be enriched by service layer
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    /**
     * Convert OrderItem entity to OrderItemResponseDTO with product name
     */
    public OrderItemResponseDTO toItemResponseDTO(OrderItem item, String productName) {
        if (item == null) {
            return null;
        }

        return new OrderItemResponseDTO(
                item.getId(),
                item.getProductId(),
                productName,
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    /**
     * Convert OrderRequestDTO to Order entity (without items)
     */
    public Order toEntity(OrderRequestDTO requestDTO) {
        if (requestDTO == null) {
            return null;
        }

        Order order = new Order();
        // spostato nel security context
        //order.setUserId(requestDTO.getUserId());
        order.setDeliveryAddress(requestDTO.getDeliveryAddress());

        return order;
    }

    /**
     * Convert OrderItemRequestDTO to OrderItem entity
     */
    public OrderItem toItemEntity(OrderItemRequestDTO itemDTO) {
        if (itemDTO == null) {
            return null;
        }

        OrderItem item = new OrderItem();
        item.setProductId(itemDTO.getProductId());
        item.setQuantity(itemDTO.getQuantity());

        return item;
    }

    /**
     * Convert list of Order entities to list of OrderResponseDTOs
     */
    public List<OrderResponseDTO> toResponseDTOList(List<Order> orders) {
        if (orders == null) {
            return null;
        }

        return orders.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }
}