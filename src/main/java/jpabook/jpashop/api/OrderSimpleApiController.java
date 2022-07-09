package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne)
 * Order
 * Order -> Member
 * Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;

    // Member <--> Order 양방향 연관관계 때문에 무한루프에 빠진다! --> 예외 발생
    // 그 외에도 양방향 걸리는 곳 모두 무한루프에 걸리게 한다.
    // 따라서 양방향 중 한 곳에 JsonIgnore를 걸어서 양방향을 끊어주어야 하는데 이것도 완벽하지 않다.
    // 왜냐하면 Lazy로 설정된 프로퍼티에는 프록시 객체가 들어서게 되는데,
    // 객체를 json으로 변환하는 jackson 라이브러리가 Member 객체를 기대하고 변환하려고 시도할 때
    // Member 객체가 아닌 프록시 객체를 상대하게 되면서 jackson 라이브러리가 예외를 발생 시킨다.
    // 이 경우 "Jackson DataType Hibernate5" 라이브러리를 이용하면(스프링 빈으로 등록하면) 해결된다.
    // 이 라이브러리는 프록시 객체를 인식하며, json으로 변환 시 프록시 객체를 null로 변환하거나
    // 아니면 프록시 객체가 실제 데이터를 가져오게 한다던가 해서 실제 데이터를 json으로 담거나 할 수 있게 한다.
    // "Jackson DataType Hibernate5" 라이브러리를 사용하지 않더라도 아래의 for loop에서처럼 강제 초기화해도 된다.
    // 하지만 엔티티를 외부로 노출시키는 것은 피해야 하는 것이기에 이런 방법들은 굳이 알려고 하지 않아도 된다.
    // 결국 DTO를 이용해야 한다.
    // 또한 지연 로딩(Lazy)을 피하기 위해 즉시 로딩(Eager)으로 설정하면 안된다.
    // 연관관계가 필요 없는 API에서도 항상 즉시 로딩 때문에 데이터를 조회해서 성능 문제가 발생할 수 있다.
    // 즉시 로딩으로 설정하면 성능 튜닝이 매우 어려워 진다.
    // 항상 지연 로딩을 기본으로 하고, 성능 최적화가 필요한 경우에는 페치 조인(fetch join)을 사용해라! (V3에서 설명)
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByCriteria(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // member의 아무 필드나 조회 --> Lazy 강제 로딩
            order.getDelivery().getAddress(); // delivery의 아무 필드나 조회 --> Lazy 강제 로딩
        }
        return all;
    }

    // 참고 - OSIV
    // Lazy 로딩은 영속성 컨텍스트 내에서만 가능하다.
    // 프록시를 대신 밀어 넣고, 추후에 프록시를 이용해 데이터를 불러오는 등의 기능은 영속성 컨텍스트에서 지원하기 때문일 것이다.
    // 즉 영속성 컨텍스트 상태에서만 지연 로딩이 동작한다. (변경 감지도 마찬가지)
    // 하지만 지금까지 배워온 바에 따르면, 영속성 컨텍스트의 라이프싸이클은 트랜잭션이 시작될 때와 트랜잭션이 끝날 때 그 사이에
    // 영속성 컨텍스트가 생성되었다가 사라진다는 것이다.
    // 위의 ordersV1이나 아래의 ordersV2는 트랜잭션이 전혀 없다. 그렇다면 영속성 컨텍스트 또한 존재하지 않을텐데
    // 왜 Lazy 로딩이 동작하는 것일까?
    // 사실 transaction이 없어도 영속성 컨텍스트가 생성되어 살아있다!
    // 이 기능을 OSIV(open-session-in-view)라고 한다.
    // 이 기능은 클라이언트 요청이 들어올 때 영속성 컨텍스트를 생성해서 요청이 반환될 때까지 영속성 컨텍스트를 유지시켜준다.
    // 다만 엔티티 수정은 트랜잭션이 있는 계층에서만 동작한다. 트랜잭션이 없는 프레젠테이션 계층은 지연 로딩을 포함해 조회만 할 수 있다.
    // https://ykh6242.tistory.com/entry/JPA-OSIVOpen-Session-In-View%EC%99%80-%EC%84%B1%EB%8A%A5-%EC%B5%9C%EC%A0%81%ED%99%94
    // https://tecoble.techcourse.co.kr/post/2020-09-11-osiv/
    // 책 "자바 ORM 표준 JPA 프로그래밍" 13장 참조
    // 기본적으로 OSIV는 켜져있지만 끌 수도 있다. (application.yml 파일등에서 설정 가능)


    // DTO 도입으로 필요한 항목만 노출하게 되어 좋아졌다.
    // 하지만 여전히 Lazy 항목을 추가로 로딩해주어야 한다.
    // order 1건당 2건의 쿼리가 추가적으로 발생한다. (Member, Delivery) --> 즉 N + 1의 문제이다.
    // 1은 첫번째 쿼리(order 컬렉션을 가져오는 쿼리)
    // N은 연관되어 있는 엔티티를 조회하는 쿼리 개수를 뜻한다.
    // N + 1 --> 1 + Member N + Delivery N = 1 + 2N  (N: order의 개수)
    // (단 지연로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 엔티티의 경우 쿼리를 생략한다.)
    // 즉, order가 2개이면 최대 총 쿼리는 5건이라는 것이다. (V1과 쿼리수는 같다)
    // LAZY를 EAGER로 변경하는 것도 해결책은 되지 못한다.
    // 그것도 마찬가지로 order를 먼저 가지고 오고 그 후에 EAGER인 항목들을 추가로 쿼리를 날려 가져오게 되는데
    // 그것만 하더라도 이미 쿼리의 개수에서 LAZY와 큰 차이가 없을 뿐더러,
    // 나가는 쿼리 자체도 난해하기 때문에 유지보수하기가 어려워진다.
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByCriteria(new OrderSearch());
        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    // fetch join으로 order, member, delivery를 한번에 가져오기 때문에
    // 더 이상 member와 delivery 정보를 위해 추가적으로 sql을 날리지 않는다. (SimpleOrderDto로 변환 시)
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithMemberDelivery();
        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    @Data
    static class SimpleOrderDto {
        private Long orderId;
        private String memberName;
        private LocalDateTime orderDate;
        private OrderStatus orderStatus;
        private Address deliveryAddress;

        // Dto가 엔티티를 파라미터로 받는 것은 크게 문제되지 않는다.
        // 크게 중요치 않은 곳(Dto)에서 의존하는 것이므로...
        public SimpleOrderDto(Order order) {
            orderId = order.getId();

            // Lazy 로딩 - 영속성 컨텍스트에서 해당 member를 찾아보고 없으면 sql 날린다.
            // 하지만 fetch join의 방법으로 order에 member도 포함되어 있다면 추가적인 sql을 날리지 않는다. --> 매우 추천
            memberName = order.getMember().getName();

            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();

            // Lazy 로딩 또는 fetch join으로 끝
            deliveryAddress = order.getDelivery().getAddress();
        }
    }



}
