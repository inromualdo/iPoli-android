package io.ipoli.android.common.job

import android.content.Context
import io.ipoli.android.Constants
import io.ipoli.android.MyPoliApp
import io.ipoli.android.achievement.usecase.UpdatePlayerStatsUseCase
import io.ipoli.android.common.di.BackgroundModule
import io.ipoli.android.common.notification.QuickDoNotificationUtil
import io.ipoli.android.common.view.AppWidgetUtil
import io.ipoli.android.pet.usecase.LowerPlayerStatsUseCase
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import space.traversal.kapsule.Kapsule

class ResetDayJob : FixedDailyJob(ResetDayJob.TAG) {

    override fun doRunJob(params: Params): Result {

        val kap = Kapsule<BackgroundModule>()
        val playerRepository by kap.required { playerRepository }
        val lowerPlayerStatsUseCase by kap.required { lowerPlayerStatsUseCase }
        val updatePlayerStatsUseCase by kap.required { updatePlayerStatsUseCase }
        val sharedPreferences by kap.required { sharedPreferences }
        kap.inject(MyPoliApp.backgroundModule(context))

        val player = playerRepository.find()!!

        val oldPet = player.pet

        val newPlayer = lowerPlayerStatsUseCase.execute(LowerPlayerStatsUseCase.Params())
        val newPet = newPlayer.pet

        if (oldPet.isDead != newPet.isDead) {
            updatePlayerStatsUseCase.execute(
                UpdatePlayerStatsUseCase.Params(
                    player = playerRepository.find()!!,
                    eventType = UpdatePlayerStatsUseCase.Params.EventType.PetDied
                )
            )
        }

        if (newPlayer.isDead) {
            sharedPreferences.edit().putBoolean(Constants.KEY_PLAYER_DEAD, true).commit()
        }

        GlobalScope.launch(Dispatchers.Main) {

            if (newPlayer.isDead) {
                QuickDoNotificationUtil.showDefeated(context)
            }

            AppWidgetUtil.updateAgendaWidget(context)
            AppWidgetUtil.updateHabitWidget(context)
        }

        return Result.SUCCESS
    }

    companion object {
        const val TAG = "job_reset_day_tag"
    }
}

interface ResetDayScheduler {
    fun schedule()
}

class AndroidResetDayScheduler(private val context: Context) : ResetDayScheduler {

    override fun schedule() {
        GlobalScope.launch(Dispatchers.IO) {

            val kap = Kapsule<BackgroundModule>()
            val playerRepository by kap.required { playerRepository }
            kap.inject(MyPoliApp.backgroundModule(this@AndroidResetDayScheduler.context))

            val p = playerRepository.find()

            requireNotNull(p)

            val t = p!!.preferences.resetDayTime
            FixedDailyJobScheduler.schedule(ResetDayJob.TAG, t)
        }
    }

}