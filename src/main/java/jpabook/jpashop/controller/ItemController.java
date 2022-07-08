package jpabook.jpashop.controller;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/items/new")
    public String createForm(Model model) {
        model.addAttribute("form", new BookForm());
        return "items/createItemForm";
    }

    @PostMapping("/items/new")
    public String create(BookForm form) {
        Book book = new Book();
        // setter를 다 제거하고 Book의 factory 메소드를 이용해 인스턴스를 생성하는 것이 더 나은 방법이다.
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());

        itemService.saveItem(book);

        return "redirect:/items";
    }

    @GetMapping("/items")
    public String list(Model model) {
        List<Item> items = itemService.findItems();
        model.addAttribute("items", items);
        return "items/itemList";
    }

    @GetMapping("items/{itemId}/edit")
    public String updateItemForm(@PathVariable("itemId") Long itemId, Model model) {
        Book item = (Book) itemService.findOne(itemId); // 예제를 간단히 하기 위해 언제나 Book이라고 가정

        BookForm form = new BookForm();
        form.setId(item.getId());
        form.setName(item.getName());
        form.setPrice(item.getPrice());
        form.setStockQuantity(item.getStockQuantity());
        form.setAuthor(item.getAuthor());
        form.setIsbn(item.getIsbn());

        model.addAttribute("form", form);

        return "items/updateItemForm";
    }

    @PostMapping("items/{itemId}/edit")
    public String updateItem(@PathVariable("itemId") Long itemId, @ModelAttribute("form") BookForm form) {

       /* Book book = new Book();

        book.setId(form.getId());
        book.setName(form.getName());
        book.setPrice(form.getPrice());
        book.setStockQuantity(form.getStockQuantity());
        book.setAuthor(form.getAuthor());
        book.setIsbn(form.getIsbn());*/
        // book == 준영속 엔티티
        // 새로운 엔티티이지만, 영속성 컨텍스트 입장에서 본다면 id가 있고(데이터 베이스에 한번 다녀온 엔티티)
        // 영속성 컨텍스트에 속하지 않으므로 준영속 엔티티이다.
        // 영속성 컨텍스트가 관리하고 있지 않으므로 (적어도 이 상태로는) dirty checking이 안된다!
        // 그럼, JPA가 이것을 update하게 하려면...?
        // 1. dirty checking
        // --> ItemService.updateItem 메소드 참조
        // 2. 병합(merge) 사용
        // 병합은 준영속 상태의 엔티티를 영속 상태로 변경한다!
        // --> ItemRepository.save 메소드 참조
        // 엔티티를 변경할 때는 항상 변경 감지를 사용하자!
        // 이유는 ItemRepository.save, ItemService.updateItem에 주석으로 적어 놓았다.


        // 해당 bookId가 변경되어 보내질 가능성이 있으므로 보안상 안전하지 않다.
        // service 단에서 해당 book이 현재 user에게 수정 권한이 있는 book인지 확인한는 로직이 필요하다.
        // 암튼 주의!
        //itemService.saveItem(book);


        // 컨트롤러에서 어설프게 엔티티를 생성하지 말자.
        // 트랜잭션이 있는 서비스 계층에 식별자 id와 변경할 데이터를 명확하게 전달하자. (파라미터 or dto)
        // 트랜잭션이 있는 서비스 계층에서 영속 상태의 엔티티를 조회하고, 엔티티의 데이터를 직접 변경하자.
        // 트랜잭션 커밋 시점에 변경 감지가 실행된다.
        // 아래와 같이...
        itemService.updateItem(itemId, form.getName(), form.getPrice(), form.getStockQuantity());

        return "redirect:/items";
    }

}
