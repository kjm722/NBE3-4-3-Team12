package com.example.backend.test.group;

import com.example.backend.domain.category.entity.Category
import com.example.backend.domain.category.entity.CategoryType
import com.example.backend.domain.category.repository.CategoryRepository
import com.example.backend.domain.group.controller.GroupController;
import com.example.backend.domain.group.dto.GroupRequestDto;
import com.example.backend.domain.group.dto.GroupResponseDto;
import com.example.backend.domain.group.entity.Group
import com.example.backend.domain.group.entity.GroupStatus;
import com.example.backend.domain.group.repository.GroupRepository
import com.example.backend.domain.group.service.GroupService;
import com.example.backend.domain.groupcategory.GroupCategory
import com.example.backend.domain.member.entity.Member
import com.example.backend.domain.member.repository.MemberRepository;
import com.example.backend.global.util.TestTokenProvider
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GroupControllerTest {

    @Autowired
    private lateinit var groupService : GroupService

    @Autowired
    private lateinit var mvc : MockMvc

    companion object {
        private lateinit var accessToken: String

        @JvmStatic
        @BeforeAll
        fun setUp(@Autowired tokenProvider: TestTokenProvider,
                  @Autowired memberRepository: MemberRepository,
                  @Autowired groupRepository: GroupRepository,
                  @Autowired categoryRepository: CategoryRepository
        ) {
            val member = Member(1L, "testUser", "test@test.com")
            memberRepository.save(member)

            accessToken = tokenProvider.generateMemberAccessToken(
                member.id, member.nickname, member.email
            )
            val category = Category("testCategory", CategoryType.STUDY)
            categoryRepository.save(category)
            val categories : MutableList<Category> = categoryRepository.findAll()

            for (i in 0 until 5){
                val group = Group("title$i","description$i",member,GroupStatus.RECRUITING,5)
                val groupCategories : MutableList<GroupCategory> = categories.map { category -> GroupCategory(group,category) }.toMutableList()
                group.addGroupCategories(groupCategories)
                groupRepository.save(group)

            }
        }
    }

    @Test
    @DisplayName("그룹 생성")
    fun t1() {
        val resultActions : ResultActions = mvc.perform(
                post("/groups")
                        .cookie(Cookie("accessToken",accessToken))
                        .content("""
                                {
                                  "title": "제목1",
                                  "description": "내용1",
                                  "maxParticipants":5,
                                  "categoryIds": [1],
                                  "status":"RECRUITING"
                                }
                                """)
                        .contentType(MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        val groupResponseDto : GroupResponseDto = groupService.create(
            GroupRequestDto(
                "제목1",
                "내용1",
                5,
                Arrays.asList(1L),
                GroupStatus.RECRUITING),1L);
        resultActions.andExpect(handler().handlerType(GroupController::class.java))
                .andExpect(handler().methodName("createGroup"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value(groupResponseDto.title))
                .andExpect(jsonPath("$.description").value(groupResponseDto.description))
                .andExpect(jsonPath("$.memberId").value(groupResponseDto.memberId))
                .andExpect(jsonPath("$.maxParticipants").value(groupResponseDto.maxParticipants))
                .andExpect(jsonPath("$.category").isArray())
                .andExpect(jsonPath("$.category[0].id").value(1L))
                .andExpect(jsonPath("$.status").value(Matchers.equalTo("RECRUITING")))
    }

    @Test
    @DisplayName("그룹 전체 조회")
    fun t2() {
        val resultActions : ResultActions = mvc.perform(
                get("/groups")
                        .contentType( MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(GroupController::class.java))
                .andExpect(handler().methodName("listGroups"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(Matchers.greaterThan(0)))
    }

    @Test
    @DisplayName("그룹 특정 조회")
    fun t3() {
        val resultActions : ResultActions = mvc.perform(
                get("/groups/{id}",1L)
                        .contentType( MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(GroupController::class.java))
                .andExpect(handler().methodName("getGroup"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.description").isNotEmpty())
                .andExpect(jsonPath("$.memberId").isNotEmpty())
                .andExpect(jsonPath("$.maxParticipants").isNotEmpty())
                .andExpect(jsonPath("$.category").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
    }

    @Test
    @DisplayName("그룹 수정")
    fun t4() {
        val resultActions : ResultActions = mvc.perform(
                put("/groups/{id}",1L)
                        .cookie( Cookie("accessToken",accessToken))
                        .content("""
                                {
                                  "title": "제목2",
                                  "description": "내용3",
                                  "maxParticipants":6,
                                  "groupStatus":"RECRUITING"
                                }
                                """)
                        .contentType( MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(GroupController::class.java))
                .andExpect(handler().methodName("modifyGroup"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("제목2"))
                .andExpect(jsonPath("$.description").value("내용3"))
                .andExpect(jsonPath("$.maxParticipants").value(6))
                .andExpect(jsonPath("$.status").value("RECRUITING"))
    }

    @Test
    @DisplayName("그룹 삭제")
    fun t5() {
        val resultActions : ResultActions = mvc.perform(
                delete("/groups/{id}",1L)
                        .cookie( Cookie("accessToken",accessToken))
                        .contentType( MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(GroupController::class.java))
                .andExpect(handler().methodName("deleteGroup"))
                .andExpect(status().isOk)
    }

    @Test
    @DisplayName("그룹 참가")
    fun t6() {
        val resultActions : ResultActions = mvc.perform(
            post("/groups/join")
                .cookie(Cookie("accessToken",accessToken))
                .content("""
                    {
                        "groupId": 1,
                        "memberId": 1
                    }
                """)
                .contentType( MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(GroupController::class.java))
            .andExpect(handler().methodName("joinGroup"))
            .andExpect(status().isOk)
    }

    @Test
    @DisplayName("유저가 속한 그룹 조회")
    fun t7() {
        val resultActions : ResultActions = mvc.perform(
            get("/groups/member")
                .cookie( Cookie("accessToken",accessToken))
                .contentType( MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
        ).andDo(print())

        resultActions.andExpect(handler().handlerType(GroupController::class.java))
            .andExpect(handler().methodName("getGroupByMember"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(Matchers.greaterThan(0)))
    }
}
