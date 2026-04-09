package com.marcfradera.shooterranking.ui.fragments

import android.content.Intent
import android.os.Bundle
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
import com.marcfradera.shooterranking.ui.adapters.EquipsAdapter
import com.marcfradera.shooterranking.ui.viewmodel.EquipsLiveDataViewModel
import kotlinx.coroutines.launch
import android.widget.ProgressBar
import android.widget.TextView

class EquipsFragment : Fragment(R.layout.fragment_recycler_screen) {

    private var _binding: FragmentRecyclerScreenBinding? = null
    private val binding get() = _binding!!

    private val vm by viewModels<EquipsLiveDataViewModel>()
    private val shared by activityViewModels<NavigationSharedViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecyclerScreenBinding.bind(view)

        val adapter = EquipsAdapter(
            onClick = {
                shared.setEquip(it.equip.id_equip, it.equip.nom_equip)
                findNavController().navigate(R.id.action_equips_to_ranking)
            },
            onEdit = {
                showEditEquipDialog(
                    idEquip = it.equip.id_equip,
                    initialNomEquip = it.equip.nom_equip
                )
            },
            onDelete = {
                showDeleteEquipDialog(
                    idEquip = it.equip.id_equip,
                    nomEquip = it.equip.nom_equip
                )
            }
        )

        val temporadaLabel = shared.selection.value?.temporadaLabel.orEmpty()
        binding.titleText.text =
            if (temporadaLabel.isBlank()) "EQUIPS"
            else "EQUIPS: $temporadaLabel"

        binding.backButton.visibility = View.VISIBLE
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.settingsButton.setOnClickListener {
            showSettingsDialog()
        }

        binding.primaryButton.text = "Afegir equip"
        binding.primaryButton.isEnabled = true
        binding.subtitleText.visibility = View.GONE

        binding.primaryButton.setOnClickListener {
            showCreateEquipDialog()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        vm.state.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            state.error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isNotBlank()) {
            vm.load(temporadaId)
        }
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

    private fun showCreateEquipDialog() {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isBlank()) {
            Toast.makeText(requireContext(), "No hi ha temporada seleccionada", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }

        val nomEdit = EditText(context).apply {
            hint = "Nom de l'equip"
        }

        container.addView(nomEdit)

        MaterialAlertDialogBuilder(context)
            .setTitle("Afegir equip")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nom = nomEdit.text.toString().trim()

                        if (nom.isBlank()) {
                            nomEdit.error = "Introdueix un nom"
                            return@setOnClickListener
                        }

                        vm.create(
                            temporadaId = temporadaId,
                            nomEquip = nom,
                            onDone = { dialog.dismiss() },
                            onError = {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                dialog.show()
            }
    }
    private fun showEquipOptionsDialog(
        idEquip: String,
        nomEquip: String
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(nomEquip)
            .setItems(arrayOf("Editar", "Eliminar")) { _, which ->
                when (which) {
                    0 -> showEditEquipDialog(
                        idEquip = idEquip,
                        initialNomEquip = nomEquip
                    )
                    1 -> showDeleteEquipDialog(
                        idEquip = idEquip,
                        nomEquip = nomEquip
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditEquipDialog(
        idEquip: String,
        initialNomEquip: String
    ) {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isBlank()) {
            Toast.makeText(requireContext(), "No hi ha temporada seleccionada", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val context = requireContext()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }

        val nomEdit = EditText(context).apply {
            hint = "Nom de l'equip"
            setText(initialNomEquip)
            setSelection(text.length)
        }

        container.addView(nomEdit)

        MaterialAlertDialogBuilder(context)
            .setTitle("Editar equip")
            .setView(container)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nom = nomEdit.text.toString().trim()

                        if (nom.isBlank()) {
                            nomEdit.error = "Introdueix un nom"
                            return@setOnClickListener
                        }

                        vm.update(
                            idEquip = idEquip,
                            temporadaId = temporadaId,
                            nomEquip = nom,
                            onDone = { dialog.dismiss() },
                            onError = {
                                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun showDeleteEquipDialog(idEquip: String, nomEquip: String) {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()

        vm.loadDeletePreview(
            idEquip = idEquip,
            onDone = { preview ->
                val message = buildString {
                    append("Vols eliminar l'equip \"$nomEquip\"?\n\n")
                    append("S'eliminara el seguent: \n")
                    append("Jugadores: ${preview.jugadors.size}\n")
                    append("Sessions totals: ${preview.sessionsCount}\n\n")
                    append("Aquesta acció no es pot desfer.")
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Confirmar eliminació")
                    .setMessage(message)
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Eliminar") { _, _ ->
                        vm.delete(
                            idEquip = idEquip,
                            temporadaId = temporadaId,
                            onDone = {
                                Toast.makeText(
                                    requireContext(),
                                    "Equip eliminat",
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