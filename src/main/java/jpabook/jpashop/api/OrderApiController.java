package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 여기서는 OneToMany 관계가 있는 OrderItem 또한 포함하는 Api를 작성한다.
 * xToOne(ManyToOne, OneToOne)
 * Order -> Member
 * Order -> Delivery
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

    // 엔티티 노출을 피하기 위해 Dto로 변환한다.
    // 여기서 핵심은 한 단계 더 들어간다.
    // OrderDto라는 내부 필드에도 혹시 엔티티 타입이 없는지 확인해야 한다.
    // List<OrderItem> 타입의 필드가 있는데 이것도 또한 엔티티 노출을 하게한다.
    // List<OrderItemDto> 타입등으로 변경해 주어야 한다.
    // 즉 Dto 내부의 어떤 필드에서도 엔티티를 사용하면 안된다.
    // 이 V2는 다 좋은데 단점이 있다. 쿼리가 어마무시하게 나간다는 점이다.
    // 연관되어 있는 엔티티 그래프를 탐색하기 위해 탐색할 때마다 쿼리가 나가게 된다.
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByCriteria(new OrderSearch());
        return orders.stream().map(OrderDto::new).collect(Collectors.toList());
    }

    // fetch join을 이용하여 필요한 객체를 한꺼번에 DB로부터 가져온다. (쿼리 1회)
    // 또한 JPQL에 distinct 키워드를 사용하여 중복(id가 같은)되는 Order 엔티티를 제거하였다.
    // V2와 V3의 컨트롤러단 코드는 거의 동일하다. 다만 리포지토리에서 호출하는 메서드가 다를뿐이다.
    // 겉으로는 아주 작은 차이지만 JPA의 마법으로 큰 차이가 나는 것이다.
    // 그것도 매우 간단하게 말이다. fetch join과 함께 탐색할 엔티티를 지정만 해 주면 된다.
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        return orders.stream().map(OrderDto::new).collect(Collectors.toList());
    }

    @Getter
    static class OrderDto {

        private Long orderId;
        private String memberName;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address deliveryAddress;

        // 엔티티를 노출하므로 이것도 Dto로 변환해야 한다.
        // private List<OrderItem> orderItems;

        // Dto로 변환하였다.
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            memberName = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            deliveryAddress = order.getDelivery().getAddress();

            // orderItems = order.getOrderItems();
            // 불행히도 OrderItem이라는 엔티티를 지정함으로써
            // OrderItems 내부의 다른 연관관계도 줄줄이 가져오려고 하는 ... 불상사가 생겨버린다.
            // 또한 엔티티를 그대로 노출하게 된다.
            // 엔티티에 대한 의존을 완전히 끊어야 한다.

            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new).collect(Collectors.toList());
        }
    }

    @Getter
    static class OrderItemDto {

        private String itemName;    // 상품 명
        private int orderPrice;     // 주문 가격
        private int count;          // 주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }
}
