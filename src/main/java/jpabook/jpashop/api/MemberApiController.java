package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    // 엔티티는 외부에 노출되면 안된다! (Member 엔티티가 파라미터로 들어있다.)
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    // Member 엔티티 대신 Dto를 받는다.
    // 외부에서 들어오는 데이터 및 로직은 엔티티와 분리되어야 한다.
    // 엔티티가 변경되어도 API spec은 변경되지 않는다.
    // 또한 엔티티의 어떤 값이 외부로 부터 들어오는지 파악하기가 쉽지 않다. (필드가 너무 많다)
    // Dto로 필드를 제한해 주면 명확하게 들어오는 값을 파악할 수 있다.
    // 예를들어, CreateMemberRequest는 name 필드만 받는다는 것이 아주 명확히 드러나 있다.
    // 그리고 같은 엔티티 대상으로 값이 들어온다고 하더라도,
    // API에 따라 검증 로직이 달라질 수 있다. 따라서 검증 로직을 엔티티에 넣는 것은
    // 아주 딱딱한 디자인이며 다양한 API에 대응하기 불가능하다.
    // API마다 Dto를 다르게 가져간다면, 검증 로직 등도 다르게 설정 가능하기에 아주 유연하게 대응할 수 있다.
    // 이것은 들어오는 파라미터뿐만 아니라 리턴 값 또한 마찬가지이다.
    // 다만 Dto와 엔티티의 매핑 로직은 추가로 필요하다.
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {

        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

}
