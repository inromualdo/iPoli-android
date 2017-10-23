package io.ipoli.android.reminder.view.picker

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import io.ipoli.android.R
import io.ipoli.android.common.ViewUtils
import io.ipoli.android.common.di.Module
import io.ipoli.android.common.mvi.ViewStateRenderer
import io.ipoli.android.common.view.MviDialogController
import io.ipoli.android.common.view.string
import io.ipoli.android.iPoliApp
import kotlinx.android.synthetic.main.dialog_reminder_picker.view.*
import space.traversal.kapsule.Injects
import space.traversal.kapsule.inject
import space.traversal.kapsule.required

typealias TimeUnitConverter = java.util.concurrent.TimeUnit

/**
 * Created by Venelin Valkov <venelin@ipoli.io>
 * on 10/4/17.
 */

enum class TimeUnit(val minutes: Long) {

    MINUTES(1),
    HOURS(60),
    DAYS(TimeUnitConverter.DAYS.toMinutes(1)),
    WEEKS(TimeUnitConverter.DAYS.toMinutes(7))
}

data class ReminderViewModel(val message: String, val minutesFromStart: Long)

class ReminderPickerDialogController :
    MviDialogController<ReminderPickerViewState, ReminderPickerDialogController, ReminderPickerDialogPresenter, ReminderPickerIntent>
    , ViewStateRenderer<ReminderPickerViewState>, Injects<Module> {

    private var listener: ReminderPickedListener? = null

    private var reminder: ReminderViewModel? = null

    constructor(listener: ReminderPickedListener, selectedReminder: ReminderViewModel? = null) : super() {
        this.listener = listener
        this.reminder = selectedReminder
    }

    protected constructor() : super()

    protected constructor(args: Bundle?) : super(args)

//    override fun loadReminderData(): Observable<ReminderViewModel> =
//        Observable.just(reminder != null)
//            .filter { !isRestoring && it }.map { reminder!! }
//
//    override fun loadNewReminderData(): Observable<Unit> =
//        Observable.just(Unit)
//            .filter { !isRestoring && reminder == null }

//    override fun pickReminderIntent() = pickReminderSubject
//
//    override fun messageChangeIntent() = messageChangeSubject
//
//    override fun predefinedValueChangeIntent() = predefinedValueChangeSubject
//
//    override fun customTimeChangeIntent() = customTimeChangeSubject
//
//    override fun timeUnitChangeIntent() = timeUnitChangeSubject

    private val presenter by required { reminderPickerPresenter }

    override fun createPresenter() = presenter

    override fun render(state: ReminderPickerViewState, view: View) {

        when (state.type) {
            ReminderPickerViewState.StateType.NEW_REMINDER -> {
                ViewUtils.showViews(view.predefinedTimes)
                ViewUtils.hideViews(view.customTimeContainer)
                view.message.setText(state.message)
                view.message.setSelection(state.message.length)

                val predefinedTimesAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_item, state.predefinedValues)
                predefinedTimesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                view.predefinedTimes.adapter = predefinedTimesAdapter
                view.predefinedTimes.setSelection(state.predefinedIndex!!)
            }

            ReminderPickerViewState.StateType.EDIT_REMINDER -> {
                showCustomTimeViews(view)
                view.message.setText(state.message)
                view.message.setSelection(state.message.length)

                val customTimeAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_item, state.timeUnits)
                customTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                view.customTimeUnits.adapter = customTimeAdapter
                view.customTimeUnits.setSelection(state.timeUnitIndex!!)
                view.customTime.setText(state.timeValue)
            }

            ReminderPickerViewState.StateType.CUSTOM_TIME -> {
                showCustomTimeViews(view)

                val customTimeAdapter = ArrayAdapter(activity!!, android.R.layout.simple_spinner_item, state.timeUnits)
                customTimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                view.customTimeUnits.adapter = customTimeAdapter
                view.customTimeUnits.setSelection(state.timeUnitIndex!!)
                view.customTime.setText(state.timeValue)

                view.customTime.requestFocus()
                ViewUtils.showKeyboard(activity!!, view.customTime)
            }

            ReminderPickerViewState.StateType.FINISHED -> {
                ViewUtils.hideKeyboard(view)
                listener?.onReminderPicked(state.viewModel!!)
                dismissDialog()
            }

            ReminderPickerViewState.StateType.TIME_VALUE_VALIDATION_ERROR -> {
                view.customTime.error = string(R.string.invalid_reminder_time)
            }
        }
    }

    private fun showCustomTimeViews(dialogView: View) {
        ViewUtils.showViews(dialogView.customTimeContainer)
        ViewUtils.hideViews(dialogView.predefinedTimes)
    }

    override fun onContextAvailable(context: Context) {
        inject(iPoliApp.module(context, router))
    }

    override fun onCreateDialog(savedViewState: Bundle?): DialogView {

        val contentView = LayoutInflater.from(activity!!).inflate(R.layout.dialog_reminder_picker, null)

        with(contentView) {
            //            RxTextView.textChanges(message).map { it.toString() }.subscribe(messageChangeSubject)
//
//            RxAdapterView.itemSelections(predefinedTimes)
//                .skipInitialValue()
//                .subscribe(predefinedValueChangeSubject)
//
//            RxAdapterView.itemSelections(customTimeUnits)
//                .skipInitialValue()
//                .subscribe(timeUnitChangeSubject)
//
//            RxTextView.textChanges(customTime).map { it.toString() }.subscribe(customTimeChangeSubject)
        }

        val dialog = AlertDialog.Builder(activity!!)
            .setView(contentView)
            .setTitle(R.string.reminder_dialog_title)
            .setIcon(R.drawable.pet_5_head)
            .setPositiveButton(R.string.dialog_ok, null)
            .setNegativeButton(R.string.cancel, { p0, p1 -> ViewUtils.hideKeyboard(contentView) })
            .setNeutralButton(R.string.do_not_remind, { p0, p1 ->
                ViewUtils.hideKeyboard(contentView)
                listener?.onReminderPicked(null)
            })
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                //                pickReminderSubject.onNext(Unit)
            }
        }

        return DialogView(dialog, contentView)
    }

    interface ReminderPickedListener {
        fun onReminderPicked(reminder: ReminderViewModel?)
    }
}