package com.ruuvi.station.calibration.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import com.ruuvi.station.R
import com.ruuvi.station.calibration.model.CalibrationType
import com.ruuvi.station.databinding.FragmentCalibrateBinding
import com.ruuvi.station.util.extensions.describingTimeSince
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import timber.log.Timber
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

abstract class CalibrationFragment(@LayoutRes contentLayoutId: Int ): Fragment(contentLayoutId) {
    abstract val viewModel: ICalibrationViewModel

    lateinit var binding: FragmentCalibrateBinding

    private var timer: Timer? = null

    abstract val calibrationType: CalibrationType

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCalibrateBinding.bind(view)

        setupViewModel()
        setupUI()
        setupCalibrationMessage()
    }

    override fun onResume() {
        super.onResume()
        timer = Timer("CalibrateTemperatureTimer", false)
        timer?.scheduleAtFixedRate(0, 500) {
            viewModel.refreshSensorData()
        }
    }

    abstract fun setupCalibrationMessage()

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    fun setupUI() {
        binding.calibrateButton.setDebouncedOnClickListener {
            val dialog = CalibrationEditDialog.newInstance(calibrationType, viewModel.getUnit(), object : CalibrationEditListener {
                override fun onDialogPositiveClick(dialog: DialogFragment, value: Double) {
                    Timber.d("onDialogPositiveClick $value")
                    viewModel.calibrateTo(value)
                }

                override fun onDialogNegativeClick(dialog: DialogFragment) {
                    Timber.d("onDialogNegativeClick")
                }
            })
            dialog.show(requireActivity().supportFragmentManager, "calibrate")
        }

        binding.clearButton.setDebouncedOnClickListener {
            val alertDialog = AlertDialog.Builder(requireContext()).create()
            alertDialog.setTitle("Confirm")
            alertDialog.setMessage("Clear calibration settings?")
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, requireContext().getString(R.string.yes)
            ) { _, _ -> viewModel.clearCalibration() }
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, requireContext().getString(R.string.no)
            ) { _, _ -> }
            alertDialog.show()
        }
    }

    fun setupViewModel() {
        viewModel.calibrationInfoObserve.observe(viewLifecycleOwner) {info ->
            binding.originalValueTextView.text = viewModel.getStringForValue(info.rawValue)
            binding.originalUpdatedTextView.text = "(${info.updateAt?.describingTimeSince(requireContext())})"
            binding.correctedlValueTextView.text = viewModel.getStringForValue(info.calibratedValue)
            binding.correctedOffsetTextView.text = "(Offset ${info.currentOffsetString})"

            binding.correctedTitleTextView.isInvisible = !info.isCalibrated
            binding.correctedlValueTextView.isInvisible = !info.isCalibrated
            binding.correctedOffsetTextView.isInvisible = !info.isCalibrated
            binding.clearButton.isEnabled = info.isCalibrated
            binding.separatorView.isInvisible = !info.isCalibrated
        }
    }

    companion object {
        const val SENSOR_ID = "SENSOR_ID"
    }
}