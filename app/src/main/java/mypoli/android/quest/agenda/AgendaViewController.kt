package mypoli.android.quest.agenda

import android.content.res.ColorStateList
import android.graphics.Rect
import android.os.Bundle
import android.support.constraint.ConstraintLayout
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import com.mikepenz.iconics.IconicsDrawable
import kotlinx.android.synthetic.main.controller_agenda.view.*
import kotlinx.android.synthetic.main.item_agenda_quest.view.*
import mypoli.android.R
import mypoli.android.common.datetime.Time
import mypoli.android.common.redux.android.ReduxViewController
import mypoli.android.common.view.*
import org.threeten.bp.LocalDate
import timber.log.Timber

/**
 * Created by Polina Zhelyazkova <polina@ipoli.io>
 * on 1/26/18.
 */
class AgendaViewController(args: Bundle? = null) :
    ReduxViewController<AgendaAction, AgendaViewState, AgendaPresenter>(args) {


    override val presenter get() = AgendaPresenter()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.controller_agenda, container, false)
        val layoutManager =
            LinearLayoutManager(container.context, LinearLayoutManager.VERTICAL, false)
        view.agendaList.layoutManager =
            layoutManager
        view.agendaList.adapter = AgendaAdapter(
            listOf(
                QuestViewModel(
                    "Study for Machine Learning class",
                    Time.Companion.at(10, 30),
                    AndroidColor.BLUE,
                    AndroidIcon.ACADEMIC
                ),
                QuestViewModel(
                    "Ride Bike to Work",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Catch the bus to London",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                ),
                QuestViewModel(
                    "Walk",
                    Time.Companion.at(20, 30),
                    AndroidColor.DEEP_ORANGE,
                    AndroidIcon.BIKE
                ),
                QuestViewModel(
                    "Dance",
                    Time.Companion.at(8, 30),
                    AndroidColor.GREEN,
                    AndroidIcon.BUS
                )
            )
        )
        view.agendaList.scrollToPosition(5)
        view.agendaList.addOnScrollListener(
            EndlessRecyclerViewScrollListener(
                layoutManager,
                { side ->
                    Timber.d("AAA Scroll $side")
                },
                3
            )
        )

//        val touchHelper =
//            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
//                override fun onMove(
//                    recyclerView: RecyclerView?,
//                    viewHolder: RecyclerView.ViewHolder?,
//                    target: RecyclerView.ViewHolder?
//                ) = false
//
//                override fun onSwiped(viewHolder: RecyclerView.ViewHolder?, direction: Int) {
//                }
//
//            })
//
//        touchHelper.attachToRecyclerView(view.agendaList)

        return view
    }

    override fun render(state: AgendaViewState, view: View) {
    }

    interface AgendaViewModel

    data class QuestViewModel(
        val name: String,
        val startTime: Time,
        val color: AndroidColor,
        val icon: AndroidIcon
    ) : AgendaViewModel

    data class DateViewModel(val date: LocalDate) : AgendaViewModel
    data class MonthDividerViewModel(val image: Int) : AgendaViewModel
    data class EmptyDaysViewModel(val label: String) : AgendaViewModel

    enum class ItemType {
        QUEST, DATE, MONTH_DIVIDER, EMPTY_DAYS
    }

    inner class AgendaAdapter(private var viewModels: List<AgendaViewModel> = listOf()) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val vm = viewModels[holder.adapterPosition]
            val itemView = holder.itemView

            itemView.completeLine.visibility = View.GONE

            val type = ItemType.values()[getItemViewType(position)]
            when (type) {
                ItemType.QUEST -> bindQuestViewModel(itemView, vm as QuestViewModel)
                ItemType.DATE -> bindDateViewModel(itemView, vm as DateViewModel)
                ItemType.MONTH_DIVIDER -> bindMonthDividerViewModel(
                    itemView,
                    vm as MonthDividerViewModel
                )
                ItemType.EMPTY_DAYS -> bindEmptyDaysViewModel(
                    itemView,
                    vm as EmptyDaysViewModel
                )
            }

            var startScrollEv: MotionEvent? = null

            val gestureDetector =
                GestureDetectorCompat(itemView.context, object :
                    GestureDetector.SimpleOnGestureListener() {


                    override fun onScroll(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float
                    ): Boolean {

                        if (distanceX > 0) {
                            return true
                        }

                        if (startScrollEv == null) {
                            startScrollEv = e1
                        }

                        itemView.completeLine.visible = true

                        strikethroughQuestName(itemView, (e2.rawX - itemView.questName.x).toInt())

                        return true
                    }
                })

            gestureDetector.setIsLongpressEnabled(true)
            itemView.setOnTouchListener { _, event ->

                if (event.action == MotionEvent.ACTION_UP) {

                    if (startScrollEv != null) {
                        val totalWidth = itemView.width.toFloat()

                        val totalDistanceX = event.rawX - startScrollEv!!.rawX

                        if (event.rawX > totalWidth / 2 && startScrollEv!!.rawX < totalWidth / 2 && totalDistanceX > totalWidth / 3) {
                            strikethroughQuestName(itemView)
                            onCompleteItem(itemView, vm)
                        } else {
                            itemView.completeLine.visibility = View.GONE
                        }
                        startScrollEv = null
                    }
                }
                gestureDetector.onTouchEvent(event)
            }
        }

        private fun strikethroughQuestName(itemView: View, to: Int? = null) {
            val textView = itemView.questName
            val textBounds = Rect()
            textView.paint.getTextBounds(
                textView.text.toString(),
                0,
                textView.text.length,
                textBounds
            )

            val lp = itemView.completeLine.layoutParams as ConstraintLayout.LayoutParams
            lp.width = to?.let { Math.min(textBounds.right, it) } ?: textBounds.right
            itemView.completeLine.layoutParams = lp
        }

        private fun onCompleteItem(itemView: View, vm: AgendaViewController.AgendaViewModel) {
            Timber.d("AAA complete item")
        }

        private fun bindEmptyDaysViewModel(
            view: View,
            viewModel: EmptyDaysViewModel
        ) {

        }

        private fun bindMonthDividerViewModel(
            view: View,
            viewModel: MonthDividerViewModel
        ) {

        }

        private fun bindDateViewModel(
            view: View,
            viewModel: DateViewModel
        ) {

        }

        private fun bindQuestViewModel(
            view: View,
            vm: QuestViewModel
        ) {
            view.questName.text = vm.name
            view.questIcon.backgroundTintList =
                ColorStateList.valueOf(colorRes(vm.color.color500))
            view.questIcon.setImageDrawable(
                IconicsDrawable(view.context)
                    .icon(vm.icon.icon)
                    .colorRes(R.color.md_white)
                    .sizeDp(24)
            )
            view.questStartTime.text = vm.startTime.toString()
        }

        override fun getItemCount() = viewModels.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            when (viewType) {
                ItemType.QUEST.ordinal -> QuestViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_quest,
                        parent,
                        false
                    )
                )
                ItemType.DATE.ordinal -> DateViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_quest,
                        parent,
                        false
                    )
                )
                ItemType.MONTH_DIVIDER.ordinal -> MonthDividerViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_quest,
                        parent,
                        false
                    )
                )
                ItemType.EMPTY_DAYS.ordinal -> EmptyDaysViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_agenda_quest,
                        parent,
                        false
                    )
                )
                else -> null
            }

        inner class QuestViewHolder(view: View) : RecyclerView.ViewHolder(view)
        inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view)
        inner class MonthDividerViewHolder(view: View) : RecyclerView.ViewHolder(view)
        inner class EmptyDaysViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemViewType(position: Int) =
            when (viewModels[position]) {
                is QuestViewModel -> ItemType.QUEST.ordinal
                is DateViewModel -> ItemType.DATE.ordinal
                is MonthDividerViewModel -> ItemType.MONTH_DIVIDER.ordinal
                is EmptyDaysViewModel -> ItemType.EMPTY_DAYS.ordinal
                else -> super.getItemViewType(position)

            }

        fun updateAll(viewModels: List<QuestViewModel>) {
            this.viewModels = viewModels
            notifyDataSetChanged()
        }

    }


}