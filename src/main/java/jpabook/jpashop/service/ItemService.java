package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }


//            itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity())

    @Transactional
    public void updateItem(Long itemId, String name, int price, int stockQuantity) {
        Item findItem = itemRepository.findOne(itemId);
        findItem.setName(name);
        findItem.setPrice(price);
        findItem.setStockQuantity(stockQuantity);
        // ... 나머지 파라미터 채움...
        // 이제 아무것도 호출할 필요 없다.
        // repository로부터 아이템을 찾아와서 엔티티가 영속성 컨텍스트에 속해 있기 때문에
        // dirty checking이 동작하게 된다.
        // 따라서 업데이트는 자동으로 수행된다. (메소드의 마지막에 flush와 commit이 자동 수행)
        // 업데이트 로직은 이러한 방식으로 하는 것이 좋다.
        // merge를 사용하게 되면 실수할 가능성이 커진다. ItemRepository.save를 참조하자.
        // 엔티티를 변경할 때는 항상 변경 감지를 사용하자.

        // 또 하나 포인트
        // 상기의 코드처럼 setter로 값을 변경하는 것 보다는
        // 따로 메소드를 정의하여 변경하는 것이 낫다.
        // 그래야 검증 비즈니스 등도 들어가서 인스턴스의 정합성을 유지할 수 있다.
        // findItem.removeStock 처럼...

        // 또 하나 포인트
        // parameter가 많아지면 차라리 Dto로 따로 정의해서 쓰는 것이 낫다.
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }

}
