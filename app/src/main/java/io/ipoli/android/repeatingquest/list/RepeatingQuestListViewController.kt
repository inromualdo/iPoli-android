package io.ipoli.android.repeatingquest.list

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.ColorRes
import android.support.annotation.LayoutRes
import android.support.v4.widget.TextViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.ionicons_typeface_library.Ionicons
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.redux.android.ReduxViewController
import io.ipoli.android.common.text.DateFormatter
import io.ipoli.android.common.text.DurationFormatter
import io.ipoli.android.common.view.*
import io.ipoli.android.common.view.recyclerview.MultiViewRecyclerViewAdapter
import io.ipoli.android.common.view.recyclerview.RecyclerViewViewModel
import io.ipoli.android.repeatingquest.entity.repeatType
import io.ipoli.android.repeatingquest.list.RepeatingQuestListViewState.StateType.CHANGED
import kotlinx.android.synthetic.main.controller_repeating_quest_list.view.*
import kotlinx.android.synthetic.main.item_repeating_quest.view.*
import kotlinx.android.synthetic.main.item_repeating_quest_progress_indicator_empty.view.*
import kotlinx.android.synthetic.main.view_empty_list.view.*
import kotlinx.android.synthetic.main.view_loader.view.*

/**
 * Created by Polina Zhelyazkova <polina@mypoli.fun>
 * on 2/14/18.
 */
class RepeatingQuestListViewController(args: Bundle? = null) :
    ReduxViewController<RepeatingQuestListAction, RepeatingQuestListViewState, RepeatingQuestListReducer>(
        args
    ) {

    override val reducer = RepeatingQuestListReducer

    override var helpConfig: HelpConfig? =
        HelpConfig(
            R.string.help_dialog_repeating_quest_list_title,
            R.string.help_dialog_repeating_quest_list_message
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setHasOptionsMenu(true)
        val view = container.inflate(
            R.layout.controller_repeating_quest_list
        )
        view.repeatingQuestList.layoutManager =
            LinearLayoutManager(container.context, LinearLayoutManager.VERTICAL, false)
        view.repeatingQuestList.adapter = RepeatingQuestAdapter()

        view.addRepeatingQuest.onDebounceClick {
            navigateFromRoot().toAddRepeatingQuest()
        }
        view.emptyAnimation.setAnimation("empty_repeating_quest_list.json")
        return view
    }

    override fun onCreateLoadAction() =
        RepeatingQuestListAction.LoadData

    override fun onAttach(view: View) {
        super.onAttach(view)
        enableToolbarTitle()
        toolbarTitle = stringRes(R.string.drawer_repeating_quests)
    }

    override fun render(state: RepeatingQuestListViewState, view: View) {
        when (state.type) {

            CHANGED -> {
                view.loader.gone()

                if (state.showEmptyView) {
                    view.emptyContainer.visible()
                    view.emptyAnimation.playAnimation()
                    view.emptyTitle.setText(R.string.empty_repeating_quests_title)
                    view.emptyText.setText(R.string.empty_repeating_quests_text)
                } else {
                    view.emptyContainer.invisible()
                    view.emptyAnimation.pauseAnimation()
                }

                (view.repeatingQuestList.adapter as RepeatingQuestAdapter).updateAll(
                    state.toViewModels(
                        view.context
                    )
                )
            }

            else -> {
            }
        }
    }

    data class TagViewModel(val name: String, @ColorRes val color: Int)

    enum class ItemType {
        REPEATING_QUEST,
        LABEL,
        COMPLETED_REPEATING_QUEST
    }

    sealed class RepeatingQuestItemViewModel(override val id: String) : RecyclerViewViewModel {
        data class RepeatingQuestViewModel(
            override val id: String,
            val name: String,
            val tags: List<TagViewModel>,
            val icon: IIcon,
            @ColorRes val color: Int,
            val next: String,
            val completedCount: Int,
            val allCount: Int,
            val frequency: String,
            val progress: List<ProgressViewModel>
        ) : RepeatingQuestItemViewModel(id)

        data class CompletedLabel(
            val label: String
        ) : RepeatingQuestItemViewModel(label)

        data class CompletedRepeatingQuestViewModel(
            override val id: String,
            val name: String,
            val tags: List<TagViewModel>,
            val icon: IIcon,
            @ColorRes val color: Int,
            val frequency: String
        ) : RepeatingQuestItemViewModel(id)

        data class ProgressViewModel(
            @LayoutRes val layout: Int,
            @ColorInt val color: Int,
            @ColorInt val strokeColor: Int
        )
    }


    inner class RepeatingQuestAdapter :
        MultiViewRecyclerViewAdapter<RepeatingQuestItemViewModel>() {
        override fun onRegisterItemBinders() {
            registerBinder<RepeatingQuestItemViewModel.RepeatingQuestViewModel>(
                ItemType.REPEATING_QUEST.ordinal,
                R.layout.item_repeating_quest
            )
            { vm, view, _ ->

                view.rqName.text = vm.name

                if (vm.tags.isNotEmpty()) {
                    view.rqTagName.visible()
                    renderTag(view, vm.tags.first())
                } else {
                    view.rqTagName.gone()
                }

                view.rqIcon.backgroundTintList =
                    ColorStateList.valueOf(colorRes(vm.color))
                view.rqIcon.setImageDrawable(listItemIcon(vm.icon))

                view.rqNext.text = vm.next
                view.rqFrequency.text = vm.frequency

//                val progressBar = view.rqProgressBar
                val progress = view.rqProgress
//                ViewUtils.showViews(progressBar, progress)
                ViewUtils.showViews(progress)
//                progressBar.max = vm.allCount
//                progressBar.progress = vm.completedCount
//                progressBar.progressTintList = ColorStateList.valueOf(colorRes(vm.color))


                val inflater = LayoutInflater.from(view.context)
                view.rqProgressBar.removeAllViews()

                vm.progress.forEachIndexed { index, pvm ->
                    val progressView = inflater.inflate(
                        R.layout.item_repeating_quest_progress_indicator_empty,
                        view.rqProgressBar,
                        false
                    )

                    val progressViewEmptyBackground =
                        progressView.indicatorDot.background as GradientDrawable
                    progressViewEmptyBackground.setStroke(
                        ViewUtils.dpToPx(2f, view.context).toInt(),
                        pvm.strokeColor
                    )

                    progressView.indicatorLink.setBackgroundColor(pvm.strokeColor)

                    if(index == 0) {
                        progressView.indicatorLink.gone()
                    }

                    progressViewEmptyBackground.setColor(pvm.color)

                    view.rqProgressBar.addView(progressView)
                }


                @SuppressLint("SetTextI18n")
                progress.text = "${vm.completedCount}/${vm.allCount}"

                view.onDebounceClick {
                    navigateFromRoot().toRepeatingQuest(vm.id, VerticalChangeHandler())
                }

            }

            registerBinder<RepeatingQuestItemViewModel.CompletedLabel>(
                ItemType.LABEL.ordinal,
                R.layout.item_list_section
            )
            { vm, view, _ ->
                (view as TextView).text = vm.label
            }

            registerBinder<RepeatingQuestItemViewModel.CompletedRepeatingQuestViewModel>(
                ItemType.COMPLETED_REPEATING_QUEST.ordinal,
                R.layout.item_repeating_quest
            )
            { vm, view, _ ->

                view.rqName.text = vm.name

                if (vm.tags.isNotEmpty()) {
                    view.rqTagName.visible()
                    renderTag(view, vm.tags.first())
                } else {
                    view.rqTagName.gone()
                }

                view.rqIcon.backgroundTintList =
                    ColorStateList.valueOf(colorRes(vm.color))
                view.rqIcon.setImageDrawable(listItemIcon(vm.icon))
//                ViewUtils.hideViews(view.rqProgressBar, view.rqProgress)

                view.rqNext.setText(R.string.completed)
                view.rqFrequency.text = vm.frequency

                view.onDebounceClick {
                    navigateFromRoot().toRepeatingQuest(vm.id, VerticalChangeHandler())
                }

            }
        }

        private fun renderTag(view: View, tag: TagViewModel) {
            view.rqTagName.text = tag.name
            TextViewCompat.setTextAppearance(
                view.rqTagName,
                R.style.TextAppearance_AppCompat_Caption
            )

            val indicator = view.rqTagName.compoundDrawablesRelative[0] as GradientDrawable
            indicator.mutate()
            val size = ViewUtils.dpToPx(8f, view.context).toInt()
            indicator.setSize(size, size)
            indicator.setColor(colorRes(tag.color))
            view.rqTagName.setCompoundDrawablesRelativeWithIntrinsicBounds(
                indicator,
                null,
                null,
                null
            )
        }
    }

    private fun RepeatingQuestListViewState.toViewModels(context: Context): List<RepeatingQuestItemViewModel> {
        val (notCompleted, completed) = repeatingQuests!!.partition { !it.isCompleted }
        val vms = mutableListOf<RepeatingQuestItemViewModel>()
        val frequencies = stringsRes(R.array.repeating_quest_frequencies)
        vms.addAll(
            notCompleted.map {
                val next = when {
                    it.nextDate != null -> {
                        var res = stringRes(
                            R.string.repeating_quest_next,
                            DateFormatter.formatWithoutYear(context, it.nextDate)
                        )
                        res += if (it.startTime != null) {
                            " ${it.startTime.toString(shouldUse24HourFormat)} - ${it.endTime!!.toString(
                                shouldUse24HourFormat
                            )}"
                        } else {
                            " " + stringRes(
                                R.string.for_time,
                                DurationFormatter.formatShort(view!!.context, it.duration)
                            )
                        }
                        res
                    }
                    else -> stringRes(
                        R.string.repeating_quest_next,
                        stringRes(R.string.unscheduled)
                    )
                }

                val rqColor = AndroidColor.valueOf(it.color.name).color500

                val progress = it.periodProgress!!

                val complete = (0 until progress.completedCount).map { _ ->
                    RepeatingQuestItemViewModel.ProgressViewModel(
                        layout = R.layout.repeating_quest_progress_indicator_empty,
                        color = colorRes(rqColor),
                        strokeColor = colorRes(rqColor)
                    )
                }
                val incomplete = (progress.completedCount until progress.allCount).map { _ ->
                    RepeatingQuestItemViewModel.ProgressViewModel(
                        layout = R.layout.repeating_quest_progress_indicator_empty,
                        color = colorRes(colorSurfaceResource),
                        strokeColor = colorRes(rqColor)
                    )
                }

                RepeatingQuestItemViewModel.RepeatingQuestViewModel(
                    id = it.id,
                    name = it.name,
                    tags = it.tags.map { TagViewModel(it.name, it.color.androidColor.color500) },
                    icon = it.icon?.let { AndroidIcon.valueOf(it.name).icon }
                        ?: Ionicons.Icon.ion_checkmark,
                    color = rqColor,
                    next = next,
                    completedCount = progress.completedCount,
                    allCount = progress.allCount,
                    frequency = frequencies[it.repeatPattern.repeatType.ordinal],
                    progress = complete + incomplete
                )
            }
        )
        if (completed.isNotEmpty()) {
            vms.add(RepeatingQuestItemViewModel.CompletedLabel(stringRes(R.string.completed)))
        }

        vms.addAll(
            completed.map {
                RepeatingQuestItemViewModel.CompletedRepeatingQuestViewModel(
                    id = it.id,
                    name = it.name,
                    tags = it.tags.map { TagViewModel(it.name, it.color.androidColor.color500) },
                    icon = it.icon?.let { AndroidIcon.valueOf(it.name).icon }
                        ?: Ionicons.Icon.ion_checkmark,
                    color = AndroidColor.valueOf(it.color.name).color500,
                    frequency = frequencies[it.repeatPattern.repeatType.ordinal]
                )
            }
        )

        return vms
    }
}