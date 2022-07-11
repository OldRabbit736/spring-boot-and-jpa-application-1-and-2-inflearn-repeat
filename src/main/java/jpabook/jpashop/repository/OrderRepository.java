package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    // 동적 쿼리... OrderSearch에서 orderStatus나 memberName이 null인 경우
    // 쿼리 스트링 자체가 변경되어야 한다... where 절이 없어져야 하는 것이다.
    // 그래서 동적 쿼리이다.
    // 동적 쿼리 중에 JPA Criteria가 있는데... 실무에서는 잘 쓰이지 않는다고 한다. 너무 복잡함!
    // 이것을 해결한 라이브러리가 QueryDSL이다!
    public List<Order> findAll(OrderSearch orderSearch) {
        return em.createQuery(
                        "select o from Order o join o.member m" +
                                " where o.status = :status" +
                                " and m.name like :name"
                        , Order.class)
                .setParameter("status", orderSearch.getOrderStatus())
                .setParameter("name", orderSearch.getMemberName())
                .setMaxResults(1000)    // 최대 1000건
                .getResultList();
    }

    // JPA Criteria는 JPA 표준 스펙이지만 실무에서 사용하기엔 너무 복잡하다.
    // 가장 멋진 대안은 QueryDSL이다.
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name);
        }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000);
        return query.getResultList();
    }

    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                "select o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d",
                // member와 delivery를 join함과 동시에 select절에 포함.
                // "select o" 는 일반적인 sql문에서의 "select o.*, m.*, d.*" 과 같은 뜻이다.
                // order 객체의 member와 delivery 필드에 프록시 객체를 생성하지 않는다.
                // 이것을 fetch join이라고 한다.
                // 즉 order뿐만 아니라 member와 delivery 객체를 영속성 컨텍스트에 저장한다.
                // fetch라는 것은 JPQL에만 있는 명령이다.
                // N + 1 문제를 90% 해결할 수 있는 방법이다. (한 방 쿼리이기 때문에)
                // 참고로 N + 1 문제는 EAGER이든 LAZY이든 나타나는 문제이다.
                // EAGER는 그 필드를 포함하고 있는 객체에 대한 데이터를 받자 마자 자동으로 해당 필드에 대한 쿼리가 자동으로 발생하고
                // LAZY는 그 필드를 사용할 때 쿼리가 발생할 뿐이다. 추가적인 쿼리가 나타나는 건 동일하다.

                Order.class
        ).getResultList();
    }

    // join과 fetch join의 차이는?
    // https://cobbybb.tistory.com/18

    // DTO로 바로 변환
    public List<OrderSimpleQueryDto> findOrderDtos() {
        return em.createQuery(
                // new operation에 entity(o, m, d 같은 값)를 인수로 전달할 수는 있지만
                // 그 경우 들어가는 값은 식별자 값이 되어버린다.
                // 값을 하나하나 넣어주어야 한다.
                // 어차피 연관관계 엔티티의 일부 필드값만 필요하므로 일반 join을 사용하였다.
                "select new jpabook.jpashop.repository.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address)" +
                        " from Order o" +
                        " join o.member m" +
                        " join o.delivery d", OrderSimpleQueryDto.class
        ).getResultList();
    }

    // ManyToOne, OneToOne만 상대할 때와는 다르게 Collection, 즉, ManyToOne을 상대할 때는 좀 더 조심해야 한다.
    // OrderItems 때문에 Order의 개수는 뻥튀기가 되어버린다. (정확히는 조인한 order rows 개수가 order item 개수와 동일해 진다)
    // 이렇게 뻥튀기 되어버린 여러 order 객체들은 사실 같은 객체이다.
    // order의 개수 그대로 중복없이 받고 싶다면 distinct 키워드를 넣어주면 된다.
    // JPQL에서 distinct 키워드의 효과는 다음 2가지이다.
    // db에 날리는 query에 distinct를 추가해준다. (쿼리를 DB에 보낼 시점에)
    // JPA가 기준이 되는 엔티티(;루트 엔티티. 여기서는 Order) 중 하나만 남기고 나머지를 없앤다. 즉 id가 중복되는 객체를 없앤다.
    // (DB에서 결과를 받아 객체를 생성한 후)
    // 책 "자바 ORM 표준 JPA 프로그래밍" p.378 참조하면 좋다.
    // 이 fetch join의 효과로 쿼리는 1회만 발생한다.
    // 여기서는 그러나 어마어마한 단점이 있다. -- 페이징이 불가능하다!!
    // 컬렉션 페치 조인 시 페이징이 불가능하다.
    // 더 정확하게 말하자면 db 쿼리문에 페이징 관련 쿼리를 날리는 것은 불가능하고
    // 페이징 처리를 어플리케이션 단에서 진행하게 된다.
    // WARN - HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!
    // 그 이유는 다음과 같다.
    // 우리는 order를 기준으로 페이징 하길 원하지만, 사길 반환되는 row를 생각해보면
    // row의 개수는 order item의 개수가 되어 버린다.
    // 따라서 order를 기준으로 페이징하기가 매우 난감해 지는 것이다.
    // Hibernate는 이런 상황을 어떻게라도 극복하려고, 쿼리로 있는 데이터는 모두 가져오고
    // 어플리케이션단에서 페이징을 처리해버린 것이다.
    // 이런 상황이기 때문에 1:다 컬렉션이 포함되어 있는 fetch join은 쓰면 위험할 수 있다.
    // 페이징이 안되기 때문에 있는 데이터를 다 긁어오게 되기 때문이다. 부하가 매우 많이 걸릴 수 있다.
    // ManyToOne, OneToOne 걸려있는 엔티티들은 마음껏 fetch join해도 된다.
    // 그러나 OneToMany 엔티티들을 fetch join하게 되면.... 다 긁어와야 한다.
    // 결과적으로,
    // 1. 하이버네이트는 경고 로그를 남긴다.
    // 2. 모든 데이터를 DB에서 읽어온다.
    // 3. 메모리에서 페이징 해버린다. (매우 위험)
    // 자세한 내용은 "자바 ORM 표준 JPA 프로그래밍" 페치 조인 부분을 참고하자. (p. 381)
    // 참고
    // 컬렉션 페치 조인은 1개만 사용할 수 있다. 컬렉션 둘 이상에 페치 조인을 사용하면 안된다.
    // 데이터가 부정합하게 조회될 수 있다.
    // 자세한 내용은 "자바 ORM 표준 JPA 프로그래밍" 을 참고하자.
    // 결론: OneToMany 엔티티는 fetch join을... 데이터가 많을 경우 절대 하지 말자.
    public List<Order> findAllWithItem() {
        return em.createQuery(
                "select distinct o from Order o" +
                        " join fetch o.member m" +
                        " join fetch o.delivery d" +
                        " join fetch o.orderItems oi" +
                        " join fetch oi.item i", Order.class)
                // 페이징 동작은 하지만
                // 쿼리 레벨에서 동작하는 것이 아닌 애플리케이션
                // 레벨에서 동작한다.
                //.setFirstResult(1)
                //.setMaxResults(100)
                .getResultList();
    }
}

