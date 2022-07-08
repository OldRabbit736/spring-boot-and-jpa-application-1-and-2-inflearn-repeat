package jpabook.jpashop.repository;

import jpabook.jpashop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.LifecycleState;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    public void save(Item item) {
        if (item.getId() == null) {
            em.persist(item);
        } else {
            // 준영속 상태의 엔티티 받고, 영속 엔티티 반환!
            // argument로 들어온 item의 id로 db에서 엔티티를 찾아와 영속성 컨텍스트에 저장 후,
            // item의 속성을 해당 엔티티에 넣어준다.(병합한다) 그리고 영속성 컨텍스트에 있는 엔티티를 반환한다.
            // 여기서 중요한 것은 입력으로 준 item 객체가 영속 상태로 변경되는 것이 아니라
            // 반환하는 merge 객체가 영속 엔티티라는 것이다.
            // 그런데 여기서 주의!!
            // 병합(merge)는 db에서 가져온 엔티티의 모든 속성을 업데이트 한다는 것이다.
            // 따라서 argument instance의 모든 필드 값이 영속성 컨텍스트의 instance에 업데이트 된다.
            // argument isntance의 필드 중 일부가 null이라면, null로 업데이트 되는 것이다.

            // 우리는 업데이트 시 일부 필드의 값만 업데이트 하길 원한다. (거의 대부분의 경우)
            // 그렇기 때문에 업데이트 필드를 명시적으로 적어주는 접근법이 훨씬 편하며 안전하다!
            // ItemService.updateItem 메소드가 바로 그것이다. 이 방법으로 업데이트 하자!
            // merge는 이런 implicit 업데이트 방식 때문에 위험하고 실수할 가능성이 크다.
            Item merge = em.merge(item);
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                .getResultList();
    }

}
