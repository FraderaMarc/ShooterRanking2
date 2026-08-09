package com.marcfradera.shooterranking.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.databinding.FragmentRecyclerScreenBinding
import com.marcfradera.shooterranking.localization.AppSettingsDialogs
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.adapters.TemporadesAdapter
import com.marcfradera.shooterranking.ui.viewmodel.TemporadesLiveDataViewModel

class TemporadesFragment : Fragment(R.layout.fragment_recycler_screen) {

    private var _binding: FragmentRecyclerScreenBinding? = null
    private val binding get() = _binding!!

    private val vm by viewModels<TemporadesLiveDataViewModel>()
    private val shared by activityViewModels<NavigationSharedViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecyclerScreenBinding.bind(view)

        val adapter = TemporadesAdapter(
            onClick = {
                val label = "${it.temporada.any_inici}-${it.temporada.any_fi}"
                shared.setTemporada(it.temporada.id_temporada, label)
                findNavController().navigate(R.id.action_temporades_to_equips)
            },
            onEdit = {
                showEditTemporadaDialog(
                    idTemporada = it.temporada.id_temporada,
                    initialAnyInici = it.temporada.any_inici,
                    initialAnyFi = it.temporada.any_fi
                )
            },
            onDelete = {
                showDeleteTemporadaDialog(
                    idTemporada = it.temporada.id_temporada,
                    title = "${it.temporada.any_inici}-${it.temporada.any_fi}"
                )
            }
        )

        binding.titleText.text = getString(R.string.seasons).uppercase()
        binding.backButton.visibility = View.GONE

        binding.settingsButton.setOnClickListener {
            AppSettingsDialogs.showSettings(requireContext()) { logoutAndRestart() }
        }

        binding.primaryButton.text = getString(R.string.add_season)
        binding.primaryButton.isEnabled = true
        binding.subtitleText.visibility = View.GONE
        binding.primaryButton.setOnClickListener { showCreateTemporadaDialog() }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        vm.state.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            state.error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        vm.load()
    }

    private fun logoutAndRestart() {
        FirebaseAuth.getInstance().signOut()

        val launchIntent = requireContext().packageManager
            .getLaunchIntentForPackage(requireContext().packageName)

        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            requireActivity().recreate()
        }
    }

    private fun createYearFields(initialStart: Int? = null, initialEnd: Int? = null): Triple<LinearLayout, EditText, EditText> {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }
        val start = EditText(context).apply {
            hint = getString(R.string.start_year)
            inputType = InputType.TYPE_CLASS_NUMBER
            initialStart?.let { setText(it.toString()); setSelection(text.length) }
        }
        val end = EditText(context).apply {
            hint = getString(R.string.end_year)
            inputType = InputType.TYPE_CLASS_NUMBER
            initialEnd?.let { setText(it.toString()); setSelection(text.length) }
        }
        container.addView(start)
        container.addView(end)
        return Triple(container, start, end)
    }

    private fun validateYears(start: EditText, end: EditText): Pair<Int, Int>? {
        val startValue = start.text.toString().trim().toIntOrNull()
        val endValue = end.text.toString().trim().toIntOrNull()
        return when {
            startValue == null -> { start.error = getString(R.string.invalid_year); null }
            endValue == null -> { end.error = getString(R.string.invalid_year); null }
            endValue < startValue -> { end.error = getString(R.string.end_year_before_start); null }
            else -> startValue to endValue
        }
    }

    private fun showCreateTemporadaDialog() {
        val (container, start, end) = createYearFields()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_season)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val years = validateYears(start, end) ?: return@setOnClickListener
                        vm.create(
                            anyInici = years.first,
                            anyFi = years.second,
                            onDone = { dialog.dismiss() },
                            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun showEditTemporadaDialog(idTemporada: String, initialAnyInici: Int, initialAnyFi: Int) {
        val (container, start, end) = createYearFields(initialAnyInici, initialAnyFi)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.edit_season)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val years = validateYears(start, end) ?: return@setOnClickListener
                        vm.update(
                            idTemporada = idTemporada,
                            anyInici = years.first,
                            anyFi = years.second,
                            onDone = { dialog.dismiss() },
                            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun showDeleteTemporadaDialog(idTemporada: String, title: String) {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }
        val messageView = TextView(context).apply {
            text = getString(R.string.loading_data)
            val bottomPad = (12 * resources.displayMetrics.density).toInt()
            setPadding(0, 0, 0, bottomPad)
        }
        val progressBar = ProgressBar(context).apply { isIndeterminate = true }
        container.addView(messageView)
        container.addView(progressBar)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.confirm_delete)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete, null)
            .create()

        dialog.setOnShowListener {
            val deleteButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            deleteButton.isEnabled = false
            vm.loadDeletePreview(
                idTemporada = idTemporada,
                onDone = { preview ->
                    if (!dialog.isShowing) return@loadDeletePreview
                    messageView.text = buildString {
                        append(getString(R.string.delete_season_question, title)).append("\n\n")
                        append(getString(R.string.will_delete_following)).append("\n")
                        append(getString(R.string.teams_count, preview.equips.size)).append("\n")
                        append(getString(R.string.players_count, preview.jugadorsCount)).append("\n")
                        append(getString(R.string.total_sessions_count, preview.sessionsCount)).append("\n\n")
                        append(getString(R.string.cannot_undo))
                    }
                    progressBar.visibility = View.GONE
                    deleteButton.isEnabled = true
                    deleteButton.setOnClickListener {
                        deleteButton.isEnabled = false
                        vm.delete(
                            idTemporada = idTemporada,
                            onDone = {
                                dialog.dismiss()
                                Toast.makeText(requireContext(), getString(R.string.season_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                deleteButton.isEnabled = true
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                onError = {
                    if (!dialog.isShowing) return@loadDeletePreview
                    messageView.text = it
                    progressBar.visibility = View.GONE
                    deleteButton.isEnabled = false
                }
            )
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
