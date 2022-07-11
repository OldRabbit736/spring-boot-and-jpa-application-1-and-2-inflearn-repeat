package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * OneToMany
 * Order -> OrderItem
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;

    // 엔티티 직접 노출
    // OrderSimpleApiController의 v1에서 설명했다시피, 안티패턴이다.
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> orders = orderRepository.findAllByCriteria(new OrderSearch());
        // 강제 초기화
        orders.forEach(order -> {
            order.getMember().getName();
            order.getDelivery().getAddress();

            // orderItems 강제 초기화
            List<OrderItem> orderItems = order.getOrderItems();

            // OrderItem 내부의 item 강제 초기화
            orderItems.forEach(orderItem -> orderItem.getItem().getName());
        });
        return orders;
    }
}
