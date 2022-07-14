package jpabook.jpashop.repository.order.query;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderQueryRepository {

    private final EntityManager em;

    public List<OrderQueryDto> findOrderQueryDtos() {
        List<OrderQueryDto> result = findOrders();

        result.forEach(o -> {
            List<OrderItemQueryDto> orderItems = findOrderItems(o.getOrderId());
            o.setOrderItems(orderItems);
        });

        return result;
    }

    public List<OrderQueryDto> findAllByDto_optimization() {
        List<OrderQueryDto> orders = findOrders();

        List<Long> orderIds = toOrderIds(orders);

        Map<Long, List<OrderItemQueryDto>> orderItemMap = findOrderItemMap(orderIds);

        orders.forEach(order -> order.setOrderItems(orderItemMap.get(order.getOrderId())));

        return orders;
    }

    private Map<Long, List<OrderItemQueryDto>> findOrderItemMap(List<Long> orderIds) {
        List<OrderItemQueryDto> orderItems = em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto" +
                                // oi.order.id는 orderitem 테이블의 order_id 컬럼값을 사용한다.
                                "(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                " from OrderItem oi" +
                                " join oi.item i" +
                                " where oi.order.id in :orderIds", OrderItemQueryDto.class)
                .setParameter("orderIds", orderIds)
                .getResultList();

        return orderItems.stream()
                .collect(Collectors.groupingBy(OrderItemQueryDto::getOrderId));
    }

    private List<Long> toOrderIds(List<OrderQueryDto> result) {
        return result.stream().map(OrderQueryDto::getOrderId).collect(Collectors.toList());
    }

    private List<OrderItemQueryDto> findOrderItems(Long orderId) {
        // 특정 order에 대한 orderitems를 구한다.
        // 컬렉션이기에 따로 구해준다.
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderItemQueryDto" +
                                // oi.order.id는 orderitem 테이블의 order_id 컬럼값을 사용한다.
                                "(oi.order.id, i.name, oi.orderPrice, oi.count)" +
                                " from OrderItem oi" +
                                " join oi.item i" +
                                " where oi.order.id = :orderId", OrderItemQueryDto.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }

    private List<OrderQueryDto> findOrders() {
        // 받아 온 데이터로 엔티티 객체를 만든다기 보다는 바로 DTO를 만들 것이기 때문에 fetch join이 아닌 join을 사용하였다.
        // OrderQueryDto의 orderItems 필드의 경우는 컬렉션이기 때문에 이 쿼리문에 포함시키지 못한다.
        // 왜냐하면 데이터를 뻥튀기 시키기 때문에 OrderQueryDto를 만들기 위한 flat한 데이터를 만들 수 없기 때문이다.
        // 참고로 이런 쿼리문은 QueryDSL을 사용하면 간편하게 작성할 수 있다.
        return em.createQuery(
                        "select new jpabook.jpashop.repository.order.query.OrderQueryDto" +
                                "(o.id, m.name, o.orderDate, o.status, d.address)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d", OrderQueryDto.class)
                .getResultList();
    }

    public List<OrderFlatDto> findAllByDto_flat() {
        // orderItems 때문에 데이터 row 수가 뻥튀기 됨
        return em.createQuery(
                        "select new" +
                                " jpabook.jpashop.repository.order.query.OrderFlatDto" +
                                "(o.id, m.name, o.orderDate, o.status, d.address, i.name, oi.orderPrice, oi.count)" +
                                " from Order o" +
                                " join o.member m" +
                                " join o.delivery d" +
                                " join o.orderItems oi" +
                                " join oi.item i", OrderFlatDto.class)
                .getResultList();
    }
}
