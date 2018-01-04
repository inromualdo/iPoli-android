package mypoli.android.challenge

import mypoli.android.challenge.ChallengeListForCategoryViewController.ChallengeViewModel
import mypoli.android.challenge.data.Challenge
import mypoli.android.challenge.data.PredefinedChallenge
import mypoli.android.common.mvi.Intent
import mypoli.android.common.mvi.ViewState
import mypoli.android.player.Player

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/29/17.
 */
sealed class ChallengeListForCategoryIntent : Intent {
    data class ChangePlayerIntent(val player: Player) : ChallengeListForCategoryIntent()
    data class LoadData(val challengeCategory: Challenge.Category) : ChallengeListForCategoryIntent()
    data class BuyChallenge(val challenge: PredefinedChallenge) : ChallengeListForCategoryIntent()
}

data class ChallengeListForCategoryViewState(
    val type: StateType,
    val challengeCategory: Challenge.Category? = null,
    val playerGems: Int = 0,
    val viewModels: List<ChallengeViewModel> = listOf()
) : ViewState {
    enum class StateType {
        LOADING,
        PLAYER_CHANGED,
        CHALLENGE_BOUGHT,
        CHALLENGE_TOO_EXPENSIVE

    }
}