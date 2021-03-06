package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    // 엔티티를 직접 외부에 노출하고 있다.
    // 엔티티가 변경되면 API spec이 변경된다는 문제점이 있다.
    // 더 자세한 설명은 아래 saveMemberV2 설명에서 확인할 수 있다.
    // 또한 반환값 자체가 array로 되어 있기 때문에 나중에 응답값에 추가적인 필드를 넣기가 어렵다는 문제가 있다.
    // 추가적인 필드를 넣기에 좋은 구조는 {}이다.
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    // collection 등의 값을 하나의 타입으로 감싸서(Result) 반환 --> Result에 추가적인 필드를 추가하기 쉬워졌다.
    // Member 대신 MemberDto를 반환함으로써 외부로의 엔티티 노출을 막았다.
    // MemberDto는 노출할 프로퍼티만 노출한다.
    @GetMapping("/api/v2/members")
    public Result<List<MemberDto>> membersV2() {
        List<Member> findMembers = memberService.findMembers();

        List<MemberDto> collect = findMembers.stream()
                .map(member -> new MemberDto(member.getName()))
                .collect(Collectors.toList());

        return new Result<>(collect.size(), collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }

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

    // update 시 dirty checking 이용
    // 받는 값, 리턴하는 값을 명확한 타입으로 정의
    // command and query 분리 (유지보수성 증대)
    // PUT은 멱등성을 만족해야 한다. 즉 여러번 수행해도 결과는 동일해야 한다.
    @PutMapping("api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMemberRequest request) {

        // command
        memberService.update(id, request.getName());
        // query
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
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
