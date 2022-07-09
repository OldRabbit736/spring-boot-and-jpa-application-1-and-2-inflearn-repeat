package jpabook.jpashop.api;

import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            order.getMember().getName(); // member의 아무 필드나 조회 --> Lazy 강제 초기화
            order.getDelivery().getAddress(); // delivery의 아무 필드나 조회 --> Lazy 강제 초기화
        }
        return all;
    }



}
