package com.marcfradera.shooterranking.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.databinding.FragmentRecyclerScreenBinding
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.adapters.TemporadesAdapter
import com.marcfradera.shooterranking.ui.viewmodel.TemporadesLiveDataViewModel
import kotlinx.coroutines.launch

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
            onLongClick = {
                showDeleteTemporadaDialog(
                    idTemporada = it.temporada.id_temporada,
                    title = "${it.temporada.any_inici}-${it.temporada.any_fi}"
                )
            }
        )

        binding.titleText.text = "TEMPORADES"
        binding.backButton.visibility = View.GONE

        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        binding.primaryButton.text = "Afegir temporada"
        binding.primaryButton.isEnabled = true
        binding.subtitleText.visibility = View.GONE

        binding.primaryButton.setOnClickListener {
            showCreateTemporadaDialog()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        vm.state.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            state.error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        vm.load()
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Configuració")
            .setItems(arrayOf("Tancar sessió")) { _, which ->
                if (which == 0) {
                    logoutAndRestart()
                }
            }
            .setNegativeButton("Cancel·lar", null)
            .show()
    }

    private fun logoutAndRestart() {
        lifecycleScope.launch {
            FirebaseAuth.getInstance().signOut()

            val launchIntent = requireContext().packageManager
                .getLaunchIntentForPackage(requireContext().packageName)

            launchIntent?.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )

            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                requireActivity().recreate()
            }
        }
    }

    private fun showCreateTemporadaDialog() {
        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }

        val anyIniciEdit = EditText(context).apply {
            hint = "Any inici"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        val anyFiEdit = EditText(context).apply {
            hint = "Any fi"
            inputType = InputType.TYPE_CLASS_NUMBER
        }

        container.addView(anyIniciEdit)
        container.addView(anyFiEdit)

        MaterialAlertDialogBuilder(context)
            .setTitle("Afegir temporada")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val anyInici = anyIniciEdit.text.toString().trim().toIntOrNull()
                        val anyFi = anyFiEdit.text.toString().trim().toIntOrNull()

                        when {
                            anyInici == null -> anyIniciEdit.error = "Any invàlid"
                            anyFi == null -> anyFiEdit.error = "Any invàlid"
                            anyFi < anyInici -> anyFiEdit.error = "L'any final no pot ser menor"
                            else -> {
                                vm.create(
                                    anyInici = anyInici,
                                    anyFi = anyFi,
                                    onDone = { dialog.dismiss() },
                                    onError = {
                                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                )
                            }
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun showDeleteTemporadaDialog(idTemporada: String, title: String) {
        vm.loadDeletePreview(
            idTemporada = idTemporada,
            onDone = { preview ->
                val message = buildString {
                    append("Vols eliminar la temporada \"$title\"?\n\n")
                    append("Equips: ${preview.equips.size}\n")
                    append("Jugadores: ${preview.jugadorsCount}\n")
                    append("Sessions totals: ${preview.sessionsCount}\n\n")
                    append("Aquesta acció no es pot desfer.")
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirmar eliminació")
                    .setMessage(message)
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        vm.delete(
                            idTemporada = idTemporada,
                            onDone = {
                                Toast.makeText(
                                    requireContext(),
                                    "Temporada eliminada",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    .show()
            },
            onError = {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}