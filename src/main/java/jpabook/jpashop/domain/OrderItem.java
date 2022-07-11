package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
// factory 생성자 외의 방법으로 인스턴스를 생성하지 못하도록 protected로 설정하였다.
// private이 아닌 protected로 한 이유는, JPA는 접근할 수 있는 기본 생성자를 필요로하기 때문이다.
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; // 주문 당시 가격
    private int count;  // 주문 수량

    // 생성 메서드 //
    // 쿠폰 등 본 가격과 차이가 나는 가격으로 주문 되는 경우도 있으므로 item 가격을 따로 받음
    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        item.removeStock(count);
        return orderItem;
    }

    // 비즈니스 로직 //
    public void cancel() {
        getItem().addStock(count);
    }

    // 조회 로직 //
    /**
     * 주문 상품 가격 조회
     */
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }


}
