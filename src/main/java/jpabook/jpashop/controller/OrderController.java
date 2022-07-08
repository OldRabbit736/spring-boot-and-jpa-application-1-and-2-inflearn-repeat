package jpabook.jpashop.controller;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.service.ItemService;
import jpabook.jpashop.service.MemberService;
import jpabook.jpashop.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MemberService memberService;
    private final ItemService itemService;

    @GetMapping("/order")
    public String createForm(Model model) {

        List<Member> members = memberService.findMembers();
        List<Item> items = itemService.findItems();

        model.addAttribute("members", members);
        model.addAttribute("items", items);

        return "order/orderForm";
    }

    @PostMapping("/order")
    public String createOrder(
            @RequestParam("memberId") Long memberId,
            @RequestParam("itemId") Long itemId,
            @RequestParam("count") int count) {

        // 외부에서 member, item entity를 조회해서 넣어줄 수도 있지만
        // 조회가 아닌 커맨드 성 작업은 아래 방식처럼 id만 넘겨주고, service에서 entity를 찾는 방식이 낫다.
        // 그래야 service의 transaction 컨텍스트 내에서 entity가 관리될 수 있다. (entity가 영속성 컨텍스트에서 관리됨)
        // 즉 entity 관리 및 변경도 핵심 비즈니스에 속하는 것이다.
        // 또한 controller의 로직도 간편해 진다.
        Long orderId = orderService.order(memberId, itemId, count);

        //return "redirect:/orders" + orderId;
        return "redirect:/orders";
    }
}
