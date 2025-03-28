package com.example.backend.domain.group.service

import com.example.backend.domain.group.dto.GroupResponseDto
import com.example.backend.global.redis.service.RedisService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class GroupTopViewService(
    private val redisService: RedisService,
    private val groupService: GroupService
) {

    private val viewKeyPrefix = "group:views:"
    private val topGroupKey = "group:top3"

    // 상위 3개의 인기 게시글을 조회
    fun getTop3ViewedGroups(): List<GroupResponseDto> {
        val keys = redisService.getAllKeys()
        val views = keys.mapNotNull { key ->
            val groupId = key.removePrefix(viewKeyPrefix).toLongOrNull()
            val viewCount = groupId?.let { redisService.getViewCount(it) }
            groupId?.let { it to viewCount }
        }
        // 조회수를 기준으로 내림차순 정렬 후 상위 3개 선택
        val topGroupIds = views.sortedByDescending { it.second }
            .take(3)
            .map { it.first }
        return topGroupIds.map { groupId ->
            var groupResponseDto = redisService.getGroupInfo(groupId)
            // Redis에 그룹 정보가 없다면 DB에서 조회 후 Redis에 저장
            if (groupResponseDto == null) {
                val groupResponse = groupService.findGroup(groupId)
                groupResponseDto = groupResponse
                redisService.saveGroupInfo(groupId, groupResponseDto)
            }
            groupResponseDto
        }
    }

fun getSavedTop3ViewedGroups(): List<GroupResponseDto> {
    val keys = redisService.getKeys("group:top3:*")
    val topGroupIds = keys.mapNotNull { key ->
        key.replace("group:top3:", "").toLongOrNull()
    }.toMutableList()

    val topGroups = topGroupIds.mapNotNull { groupId ->
        redisService.getGroupInfo(groupId)
    }.toMutableList()

    // 현재 그룹 개수가 3개보다 적다면 추가 조회
    if (topGroups.size < 3) {
        val neededCount = 3 - topGroups.size

        // 기존 topGroupIds에 없는 새로운 인기 게시글 조회
        val additionalGroups = getTop3ViewedGroups()
            .filter { it.id !in topGroupIds } // 기존 데이터 제외
            .take(neededCount) // 부족한 개수만큼 추가

        additionalGroups.forEach { group ->
            redisService.saveGroupInfo(group.id, group) // Redis에 저장
            topGroupIds.add(group.id)
            topGroups.add(group)
        }
    }

    return topGroups
}


   @Scheduled(cron = "0 0 0 * * SUN")
   //@Scheduled(cron = "0 * * * * *") 테스트용
    fun showTop3Posts() {
        redisService.delete(topGroupKey)
        val topPosts = getTop3ViewedGroups()
        topPosts.forEachIndexed { index, group ->
            redisService.saveGroupInfo(group.id, group)
        }
    }
}
