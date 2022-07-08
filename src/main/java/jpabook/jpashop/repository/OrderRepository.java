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
}
