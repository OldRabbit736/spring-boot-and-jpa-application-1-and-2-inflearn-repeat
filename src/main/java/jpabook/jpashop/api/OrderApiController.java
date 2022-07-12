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
import org.springframework.web.bind.annotation.RequestParam;
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
    // 하지만 이 방법의 큰 단점이 있다. 페이징이 되긴 되는데, 메모리에서 수행된다는 점이다.
    // 데이터가 클 경우 장애로 이어질 수도 있다.
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        return orders.stream().map(OrderDto::new).collect(Collectors.toList());
    }


    /*
    V3의 페이징의 한계 돌파
    1. xToOne 관계를 가진 엔티티는 부담없이 페치 조인하라.
    2. 그 상태에서 offset, limit을 쿼리문에 적용하라 (xToOne 밖에 안 걸려 있으므로 주 엔티티 기준으로 offset, limit이 잘 먹는다.)
    3. batch fetch를 활성화 시킨다. 아래 2개의 방법이 있다.
        A) 글로벌 세팅: application.yml에서 spring.jpa.properties.hibernate.default_batch_fetch_size 값을 지정한다. (예: 100)
        B) 개별 세팅 : @BatchSize를 추가한다. (컬렉션은 컬렉션 필드에, 엔티티는 엔티티 클래스에)
            - 컬렉션 적용 : Order.orderItems 필드 annotation 참조
            - 엔티티 적용 : Item 클래스 annotation 참조
    4. 쿼리 호출 수가 1 + N 에서 1 + 1이 되는 매직을 경험하라.
       orderItem, item이 "in query"로 한꺼번에 각각 가져오게 된다. (List<Order>의 바운더리에 있는 orderItem, item을 한꺼번에 가져온다)
       따라서 1(Orders) + 1(OrderItems) + 1(Items) 가 되어 총 3번의 쿼리밖에 발생하지 않는다!
       물론 배치 사이즈에 비해 가져와야 하는 아이템이 많으면 쿼리가 몇 회 더 나갈수는 있다.
       예를들어 배치 사이즈가 100인데 Order의 갯수가 1000개이면, OrderItems의 in 절에 들어가는 Order Id의 갯수가 100개밖에 안되기 때문에
       OrderItems로 10번의 쿼리를 날려야 한다.
    5. 이 정도 기능을 사용하면 왠만한 상황에서 원하는 성능을 맞추는 데 문제가 없다.
    6. 이 기능을 사용안할 이유가 없는 것 같다. 프로젝트에 기본으로 깔고 가자. (글로벌 세팅)
    7. batch fetch size는 100 ~ 1000 사이를 선택하는 것을 권장한다.
        - 최저 size는 기준이 없지만 최대 size는 기준이 있다. 1000개 이하로 설정하라.
        - 데이터베이스에 따라 in 절 파라미터 갯수를 1000개로 제한하는 경우도 있기 때문이다.
        - 숫자가 높을수록 쿼리 숫자는 줄어들겠지만, DB의 순간부하를 더 높게 한다.
        - 따라서 DB나 애플리케이션 상황에 따라 적절히 쿼리 숫자와 부하를 고려해서 정해야 한다.
        - 처음 100으로 지정해서 운영하다가, 점차 늘려가면서 모니터링 하는 방법도 있다.

    결론: 이 방법을 쓰면 xToMany의 관계가 끼어있어도 페이징과 최적화(성능) 둘 다 잡을 수 있다.

    V3.1을 V3과 비교 시
    장점
        - fetch join 방식보다 쿼리가 약간 증가하지만 DB 데이터 전송량이 적다. (뻥튀기가 없다)
        - 페이징이 가능하다.
    결론
        - xToOne 관계는 부담없이 fetch join으로 쿼리 수를 줄이고 나머지 xToMany 관계는 batch fetch로 지연 로딩을 최적화 하자!
        - 페이징을 써야 하면 다른 대안이 거의 없다. 이 방법을 쓸 수 밖에 없다.
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_1(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

    }

    // ordersV3_1와 모두 동일하지만 order만 조회하고 member와 delivery를 fetch join에서 제외하였다.
    // 따라서 멘 처음 order를 조회한 후, member, delivery, order item, item 모두 batch fetch로 조회한다.
    // 즉 1(Order) + 1(Member) + 1(Delivery) + 1(OrderItem) + 1(Item) 쿼리가 발생한다.
    // 나쁘진 않다. 그러나 확실히 앞의 것보다 쿼리가 더 발생하긴 한다.
    /** 잠깐... 이거 혹시... 멘토님이 언급했던 최대한 join 없이 쿼리를 날리는 practice가 이것을 가리키는 것이었나? **/
    @GetMapping("/api/v3.2/orders")
    public List<OrderDto> ordersV3_2(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithoutMemberDelivery(offset, limit);

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

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
