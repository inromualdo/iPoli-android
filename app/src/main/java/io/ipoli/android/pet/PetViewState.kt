package io.ipoli.android.pet

import io.ipoli.android.common.mvi.Intent
import io.ipoli.android.common.mvi.ViewState

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 11/24/17.
 */
sealed class PetIntent : Intent

object LoadDataIntent : PetIntent()

data class PetViewState(
    val type: StateType = StateType.DATA_LOADED
) : ViewState {
    enum class StateType {
        LOADING, DATA_LOADED
    }
}