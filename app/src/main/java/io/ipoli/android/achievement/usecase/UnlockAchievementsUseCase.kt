package io.ipoli.android.achievement.usecase

import io.ipoli.android.achievement.Achievement
import io.ipoli.android.achievement.job.ShowUnlockedAchievementsScheduler
import io.ipoli.android.common.UseCase
import io.ipoli.android.common.datetime.Time
import io.ipoli.android.friends.usecase.SavePostsUseCase
import io.ipoli.android.player.data.Player
import io.ipoli.android.player.data.Statistics
import io.ipoli.android.player.persistence.PlayerRepository
import org.threeten.bp.LocalDate

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 6/7/18.
 */
open class UnlockAchievementsUseCase(
    private val playerRepository: PlayerRepository,
    private val showUnlockedAchievementsScheduler: ShowUnlockedAchievementsScheduler,
    private val updatePlayerStatsUseCase: UpdatePlayerStatsUseCase,
    private val savePostsUseCase: SavePostsUseCase
) : UseCase<UnlockAchievementsUseCase.Params, List<Achievement>> {

    override fun execute(parameters: Params): List<Achievement> {

        val player = if (parameters.eventType == null) parameters.player
        else updatePlayerStatsUseCase.execute(
            UpdatePlayerStatsUseCase.Params(
                player = parameters.player,
                eventType = parameters.eventType,
                currentDate = parameters.currentDate
            )
        )

        val unlockedAchievements = player.achievements.map { it.achievement }
        val newAchievements =
            (Achievement.values().toList() - unlockedAchievements).filter {
                shouldUnlock(
                    player,
                    it,
                    player.statistics
                )
            }

        if (newAchievements.isNotEmpty()) {
            val p = playerRepository.save(
                player.copy(
                    achievements = player.achievements + newAchievements.map {
                        Player.UnlockedAchievement(
                            it, Time.now(), LocalDate.now()
                        )
                    }
                )
            )
            showUnlockedAchievementsScheduler.schedule(newAchievements)
            try {
                newAchievements.forEach {
                    savePostsUseCase.execute(SavePostsUseCase.Params.AchievementUnlocked(it, p))
                }
            } catch (e: Throwable) {}
        }

        return newAchievements
    }

    private fun shouldUnlock(player: Player, achievement: Achievement, stats: Statistics): Boolean {
        return when (achievement) {
            Achievement.FIRST_QUEST_COMPLETED ->
                stats.questCompletedCount >= 1

            Achievement.COMPLETE_10_QUESTS_IN_A_DAY ->
                stats.questCompletedCountForToday >= 10

            Achievement.COMPLETE_QUEST_FOR_100_DAYS_IN_A_ROW ->
                stats.questCompletedStreak.count >= 100

            Achievement.COMPLETE_DAILY_CHALLENGE ->
                stats.dailyChallengeCompleteStreak.count >= 1

            Achievement.COMPLETE_DAILY_CHALLENGE_10_DAY_STREAK ->
                stats.dailyChallengeCompleteStreak.count >= 10

            Achievement.COMPLETE_DAILY_CHALLENGE_30_DAY_STREAK ->
                stats.dailyChallengeCompleteStreak.count >= 30

            Achievement.KEEP_PET_HAPPY_5_DAY_STREAK ->
                stats.petHappyStateStreak >= 5

            Achievement.KEEP_PET_HAPPY_15_DAY_STREAK ->
                stats.petHappyStateStreak >= 15

            Achievement.KEEP_PET_HAPPY_40_DAY_STREAK ->
                stats.petHappyStateStreak >= 40

            Achievement.AWESOMENESS_SCORE_5_DAY_STREAK ->
                stats.awesomenessScoreStreak >= 5

            Achievement.AWESOMENESS_SCORE_20_DAY_STREAK ->
                stats.awesomenessScoreStreak >= 20

            Achievement.AWESOMENESS_SCORE_50_DAY_STREAK ->
                stats.awesomenessScoreStreak >= 50

            Achievement.FOCUS_HOURS_5_DAY_STREAK ->
                stats.focusHoursStreak >= 5

            Achievement.FOCUS_HOURS_20_DAY_STREAK ->
                stats.focusHoursStreak >= 20

            Achievement.FOCUS_HOURS_50_DAY_STREAK ->
                stats.focusHoursStreak >= 50

            Achievement.PLAN_DAY_5_DAY_STREAK ->
                stats.planDayStreak.count >= 5

            Achievement.PLAN_DAY_20_DAY_STREAK ->
                stats.planDayStreak.count >= 20

            Achievement.PLAN_DAY_50_DAY_STREAK ->
                stats.planDayStreak.count >= 50

            Achievement.FIRST_REPEATING_QUEST_CREATED ->
                stats.repeatingQuestCreatedCount >= 1

            Achievement.COMPLETE_CHALLENGE ->
                stats.challengeCompletedCount >= 1

            Achievement.COMPLETE_5_CHALLENGES ->
                stats.challengeCompletedCount >= 5

            Achievement.COMPLETE_15_CHALLENGES ->
                stats.challengeCompletedCount >= 15

            Achievement.FIRST_CHALLENGE_CREATED ->
                stats.challengeCreatedCount >= 1

            Achievement.CONVERT_1_GEM ->
                stats.gemConvertedCount >= 1

            Achievement.CONVERT_20_GEMS ->
                stats.gemConvertedCount >= 20

            Achievement.CONVERT_50_GEMS ->
                stats.gemConvertedCount >= 50

            Achievement.REACH_LEVEL_10 ->
                player.level >= 10

            Achievement.REACH_LEVEL_30 ->
                player.level >= 30

            Achievement.REACH_LEVEL_50 ->
                player.level >= 50

            Achievement.HAVE_1K_LIFE_COINS_IN_INVENTORY ->
                player.coins >= 1000

            Achievement.HAVE_5K_LIFE_COINS_IN_INVENTORY ->
                player.coins >= 5000

            Achievement.HAVE_10K_LIFE_COINS_IN_INVENTORY ->
                player.coins >= 10000

            Achievement.INVITE_1_FRIEND ->
                stats.friendInvitedCount >= 1

            Achievement.INVITE_5_FRIENDS ->
                stats.friendInvitedCount >= 5

            Achievement.INVITE_20_FRIENDS ->
                stats.friendInvitedCount >= 20

            Achievement.GAIN_999_XP_IN_A_DAY ->
                stats.experienceForToday >= 999

            Achievement.FIRST_PET_ITEM_EQUIPPED ->
                stats.petItemEquippedCount >= 1

            Achievement.FIRST_AVATAR_CHANGED ->
                stats.avatarChangeCount >= 1

            Achievement.FIRST_PET_CHANGED ->
                stats.petChangeCount >= 1

            Achievement.PET_FED_WITH_POOP ->
                stats.petFedWithPoopCount >= 1

            Achievement.PET_FED ->
                stats.petFedCount >= 1

            Achievement.FEEDBACK_SENT ->
                stats.feedbackSentCount >= 1

            Achievement.BECAME_PRO ->
                stats.joinMembershipCount >= 1

            Achievement.FIRST_POWER_UP_ACTIVATED ->
                stats.powerUpActivatedCount >= 1

            Achievement.PET_REVIVED ->
                stats.petRevivedCount >= 1

            Achievement.PET_DIED ->
                stats.petDiedCount >= 1
        }
    }


    data class Params(
        val player: Player,
        val eventType: UpdatePlayerStatsUseCase.Params.EventType? = null,
        val currentDate: LocalDate = LocalDate.now()
    )

}
