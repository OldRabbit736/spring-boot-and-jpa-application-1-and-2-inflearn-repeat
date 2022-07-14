package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final OrderQueryRepository orderQueryRepository;


    /*
    정리
    - 엔티티 조회
        - 엔티티 조회 후 그대로 반환 : V1
        - 엔티티 조회 후 DTO로 변환 : V2
        - 페치 조인으로 쿼리 수 최적화 : V3
        - 컬렉션 페이징과 한계 돌파 : V3.1
            - ToOne 관계는 페치 조인으로
            - ToMany 관계인 컬렉션은 페치 조인에서 제외하고 지연 로딩을 유지하며
              hibernate.default_batch_fetch_size, @BatchSize 방식으로 최적화
    - DTO 직접 조회
        - JPA에서 DTO를 직접 조회 : V4
        - 컬렉션 조회 최적화 - ToMany 관계인 컬렉션은 IN 절을 이용해 한 꺼번에 데이터를 가져오는 방식으로 최적화 : V5
        - 플랫 데이터 최적화 - ToOne, ToMany 든 모두 JOIN 하여 한꺼번에 데이터를 가져온 후 애플리케이션에서 원하는 모양으로 직접 변환 : V6


    권장 순서
    - 엔티티 조회 방식으로 우선 접근
        - 페치 조인으로 쿼리 수를 최적화
        - 컬렉션 최적화
            - 페이징 필요 O : hibernate.default_batch_fetch_size, @BatchSize로 최적화 (v3.1)
            - 페이징 필요 X : 페치 조인 사용 (v3)
    - 엔티티 조회 방식으로 해결이 안되면 DTO 조회 방식 사용
    - DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 JdbcTemplate 사용


    참고
    - 엔티티 조회 방식은 페치 조인, hibernate.default_batch_fetch_size, @BatchSize 등의 기능을 사용하면 되므로
      코드 수정이 거의 없이 옵션만 변경하면서 다양한 성능 최적화를 시도할 수 있다.
      그리고 왠만하면 이정도로 해결이 된다. 여기서 해결이 안되면, 정말 사용량이 많은 경우일텐데,
      캐시(redis 등) 같은 것을 도입해야 맞는 것이지, DTO로 최적화 한다고 해서 ... 해결될 문제가 아닐 수도 있다.
      반면 DTO를 직접 조회하는 방식은 성능을 최적화 하거나 성능 최적화 방식을 변경할 때 많은 코드를 변경해야 한다.
    - 개발자는 성능 최적화와 코드 복잡도 사이에서 줄타기를 해야 한다. 보통 성능 최적화는 단순한 코드를 복잡한 코드로 몰고 간다.
      엔티티 조회 방식은 JPA가 많은 부분을 최적화 해주기 때문에, 단순한 코드를 유지하면서 성능을 취적화 할 수 있는 좋은 방법이다.
      반면에 DTO 조회 방식은 SQL을 직접 다루는 것과 유사하기 때문에, 둘 사이에서 줄타기를 해야만 한다.


    DTO 조회 방식의 선택지
    - DTO 조회 방식도 각각 장단점이 있다. V4, V5, V6에서 단순하게 쿼리가 1번 실행된다고 V6가 항상 좋은 방법인 것은 아니다.
    - V4는 코드가 단순하다. 읽기 쉬우며 유지보수하기도 수월할 것이다. 주문 한 건만 조회하면 이 방식을 사용해도 성능이 잘 나온다.
      Order 데이터가 한 건이면 OrderItem 쿼리도 한 건만 실행하면 된다.
    - V5는 코드가 복잡하다. 그러나 여러 주문을 한꺼번에 조회해야 하는 상황에서는 V4 방식 보다는 V5 방식을 사용해야 한다.
      왜냐하면 Order 데이터가 1000건이라고 한다면 V4는 총 쿼리 수가 1 + 1000 건 이지만, V5는 1 + 1  건이기 때문이다.
      상황에 따라 다르겠지만 운영 환경에서 100배 이상의 성능 차이가 날 수 있다.
    - V6는 완전히 다른 접근방시깅다. 쿼리가 한 건으로 끝나서 상당히 좋아보이지만, Order를 기준으로 페이징이 불가능하다.
      실무에서는 이 정도 데이터면 수백이나 수천건 단위로 페이징 처리가 필요하므로, 선택하기 어려운 방법이다.
      그리고 데이터가 많으면 중복 전송 데이터가 많아지기 때문에 V5와 성능 차이도 미미하다. 오히려 V5의 성능이 잘 나올 경우도 있을 것이다.
    - V6를 사용하기 애매한 경우가 많다고 한다. V5가 가장 무난한 선택인 것 같다. 단건 조회일 경우 V4를 사용하면 될 것 같다.
    - 그러나 사실... V5 자체도 batch fetch를 수동으로 구현한 것일 뿐이다. 왠만하면 엔티티 조회 버전을 사용하는 게 좋을 거 같다.
    */


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
    // 또한 DB에서 데이터를 일부가 아니라 모두 긁어온다.
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

    /**
     * 잠깐... 이거 혹시... 멘토님이 언급했던 최대한 join 없이 쿼리를 날리는 practice가 이것을 가리키는 것이었나?
     **/
    @GetMapping("/api/v3.2/orders")
    public List<OrderDto> ordersV3_2(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {

        List<Order> orders = orderRepository.findAllWithoutMemberDelivery(offset, limit);

        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());

    }

    /*
     JPA에서 DTO를 직접 반환하는 버전
     - DTO 로직은 화면 로직과 마찬가지이고 따라서 DTO를 반환하는 레포지토리는 "쿼리 레포지토리"로 따로 생성한다.
     - 엔티티 반환하는 "순수 레포지토리"와 "쿼리 레포지토리"는 생명, 유지보수 싸이클이 다르기 때문에 따로 관리하는 것이 좋다.

     쿼리
     - 루트 1번, 컬렉션 N번 실행 (N + 1)
     - ToOne 관계들을 루트에 포함하여 먼저 조회하고, ToMany 관계는 각각 별도로 조회한다.
        - ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
        - ToMany 관계는 조인하면 row 수를 증가시킨다.
     - row 수가 증가하지 않는 ToOne 관계는 조인으로 최적화 하기 쉬우므로 한번에 조회하고,
       ToMany 관계는 최적화 하기 어려우므로 별도의 메서드로 조회한다.

     의문점
     - 그런데 왜 여기서는 batch fetch가 작동하지 않았을까?
     - 아마도... Dto가 순수한 데이터만 저장하는 객체라서 편이 기능이 동작하지 않는듯 하다.
     - 프록시 객체로 초기화된 필드의 엔티티를 구하려 할 때,
       프록시의 어떤 마법에 의해 batch fetch가 동작하는 건 아닐지 추측해 본다.
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /*
    JPA에서 DTO를 직접 반환하는 버전 - 컬렉션 조회 최적화

    쿼리
    - 루트 1번, 컬렉션 1번 (1 + 1)
    - ToOne 관계들을 먼저 조회하고 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem을 한꺼번에 조회
    - Map을 이용해 매칭 성능 향상 (O(1))

    뭔가... batch fetch 로직을 수동으로 돌린 느낌이다.
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /*
    JPA에서 DTO를 직접 반환하는 버전 - 플랫 데이터 최적화

    한줄로 만든 DTO를 다시 분해하고 조립하는 과정을 거쳐야 한다.

    쿼리
    - 모두 조인하기 때문에 1회

    단점
    - 쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가 추가되므로
      상황에 따라 V5보다 더 느릴 수 있다.
    - 애플리케이션에서 추가 작업이 크다.
    - 페이징 불가능
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        // List<OrderFlatDto> --> List<OrderQueryDto>
        // OrderQueryDto를 key로 사용하고 OrderItemQueryDto를 value로 분류한 후
        // key에 소속되어 있는 value를 key의 orderItems에 set 하였다.
        // OrderQueryDto를 key로 사용한 덕분에 distinct한 OrderQueryDto만 key로 정리된다.
        // OrderQueryDto를 key로 사용하기 때문에 OrderQueryDto의 hash, equals 메서드가 매우 중요하다.
        // orderId 기준으로 판단하는 것으로 재 정의하였다.
        return flats.stream()
                .collect(Collectors.groupingBy(
                        o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), Collectors.toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(), e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
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
