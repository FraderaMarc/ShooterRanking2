package com.marcfradera.shooterranking.ui.fragments

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.databinding.FragmentRecyclerScreenBinding
import com.marcfradera.shooterranking.localization.AppSettingsDialogs
import com.marcfradera.shooterranking.shared.NavigationSharedViewModel
import com.marcfradera.shooterranking.ui.adapters.EquipsAdapter
import com.marcfradera.shooterranking.ui.viewmodel.EquipsLiveDataViewModel

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
                showEditEquipDialog(it.equip.id_equip, it.equip.nom_equip, it.equip.tipus_pista)
            },
            onDelete = {
                showDeleteEquipDialog(it.equip.id_equip, it.equip.nom_equip)
            }
        )

        val temporadaLabel = shared.selection.value?.temporadaLabel.orEmpty()
        binding.titleText.text = if (temporadaLabel.isBlank()) {
            getString(R.string.teams).uppercase()
        } else {
            getString(R.string.teams_title_with_season, temporadaLabel).uppercase()
        }

        binding.backButton.visibility = View.VISIBLE
        binding.backButton.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.settingsButton.setOnClickListener {
            AppSettingsDialogs.showSettings(requireContext()) { logoutAndRestart() }
        }

        binding.primaryButton.text = getString(R.string.add_team)
        binding.primaryButton.isEnabled = true
        binding.subtitleText.visibility = View.GONE
        binding.primaryButton.setOnClickListener { showCreateEquipDialog() }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        vm.state.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.data)
            state.error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isNotBlank()) vm.load(temporadaId)
    }

    private fun logoutAndRestart() {
        FirebaseAuth.getInstance().signOut()
        val launchIntent = requireContext().packageManager
            .getLaunchIntentForPackage(requireContext().packageName)
        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (launchIntent != null) startActivity(launchIntent) else requireActivity().recreate()
    }

    private fun showCreateEquipDialog() {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.no_season_selected), Toast.LENGTH_SHORT).show()
            return
        }

        val context = requireContext()
        var tipusPistaSeleccionat = "Base"
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }
        val nomEdit = EditText(context).apply { hint = getString(R.string.team_name) }
        val tipusPistaView = createTipusPistaSelector("Base", true) { tipusPistaSeleccionat = it }
        container.addView(nomEdit)
        container.addView(tipusPistaView)

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.add_team)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nom = nomEdit.text.toString().trim()
                        if (nom.isBlank()) {
                            nomEdit.error = getString(R.string.enter_name)
                            return@setOnClickListener
                        }
                        vm.create(
                            temporadaId = temporadaId,
                            nomEquip = nom,
                            tipusPista = tipusPistaSeleccionat,
                            onDone = { dialog.dismiss() },
                            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
                dialog.show()
            }
    }

    private fun showEditEquipDialog(idEquip: String, initialNomEquip: String, initialTipusPista: String) {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        if (temporadaId.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.no_season_selected), Toast.LENGTH_SHORT).show()
            return
        }
        vm.loadDeletePreview(
            idEquip = idEquip,
            onDone = { preview ->
                showEditEquipDialogContent(
                    idEquip,
                    temporadaId,
                    initialNomEquip,
                    initialTipusPista,
                    canEditTipusPista = preview.sessionsCount <= 0
                )
            },
            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        )
    }

    private fun showEditEquipDialogContent(
        idEquip: String,
        temporadaId: String,
        initialNomEquip: String,
        initialTipusPista: String,
        canEditTipusPista: Boolean
    ) {
        val context = requireContext()
        val normalizedTipusPista = when (initialTipusPista) {
            "Amateur", "Pro" -> "Amateur"
            else -> "Base"
        }

        /*
         * Si el tipo está bloqueado por sesiones existentes, conservamos el valor
         * real almacenado para que editar solamente el nombre no cambie la pista.
         */
        var tipusPistaSeleccionat = if (canEditTipusPista) {
            normalizedTipusPista
        } else {
            initialTipusPista.ifBlank { "Base" }
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, 8, pad, 0)
        }
        val nomEdit = EditText(context).apply {
            hint = getString(R.string.team_name)
            setText(initialNomEquip)
            setSelection(text.length)
        }
        val tipusPistaView = createTipusPistaSelector(
            normalizedTipusPista,
            canEditTipusPista
        ) {
            tipusPistaSeleccionat = it
        }
        container.addView(nomEdit)
        container.addView(tipusPistaView)

        if (!canEditTipusPista) {
            container.addView(TextView(context).apply {
                text = getString(R.string.court_type_locked)
                textSize = 13f
                val topMargin = (8 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, topMargin, 0, 0) }
            })
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit_team)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nom = nomEdit.text.toString().trim()
                        if (nom.isBlank()) {
                            nomEdit.error = getString(R.string.enter_name)
                            return@setOnClickListener
                        }
                        vm.update(
                            idEquip = idEquip,
                            temporadaId = temporadaId,
                            nomEquip = nom,
                            tipusPista = tipusPistaSeleccionat,
                            onDone = { dialog.dismiss() },
                            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
                dialog.show()
            }
    }

    /** UI labels are translated, while returned values stay canonical for Firestore compatibility. */
    private fun createTipusPistaSelector(
        initialTipusPista: String,
        enabled: Boolean,
        onSelected: (String) -> Unit
    ): View {
        val context = requireContext()
        val density = resources.displayMetrics.density
        val wrapper = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val title = TextView(context).apply {
            text = getString(R.string.court_type)
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, (18 * density).toInt(), 0, (8 * density).toInt())
            }
        }
        val toggleGroup = MaterialButtonToggleGroup(context).apply {
            orientation = LinearLayout.VERTICAL
            isSingleSelection = true
            isSelectionRequired = true
            isEnabled = enabled
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val baseButtonId = View.generateViewId()
        val fibaButtonId = View.generateViewId()

        fun createButton(idValue: Int, labelRes: Int): MaterialButton = MaterialButton(
            context,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            id = idValue
            text = getString(labelRes)
            isCheckable = true
            isEnabled = enabled
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        toggleGroup.addView(createButton(baseButtonId, R.string.court_base))
        toggleGroup.addView(createButton(fibaButtonId, R.string.court_amateur))

        /*
         * "Amateur" se conserva como valor interno para mantener compatibilidad
         * con equipos y sesiones ya guardados. En la interfaz ahora se muestra FIBA.
         * Los antiguos equipos "Pro" se muestran como FIBA en el selector.
         */
        toggleGroup.check(
            when (initialTipusPista) {
                "Amateur", "Pro" -> fibaButtonId
                else -> baseButtonId
            }
        )

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            onSelected(
                when (checkedId) {
                    fibaButtonId -> "Amateur"
                    else -> "Base"
                }
            )
        }
        wrapper.addView(title)
        wrapper.addView(toggleGroup)
        return wrapper
    }

    private fun showDeleteEquipDialog(idEquip: String, nomEquip: String) {
        val temporadaId = shared.selection.value?.temporadaId.orEmpty()
        vm.loadDeletePreview(
            idEquip = idEquip,
            onDone = { preview ->
                val message = buildString {
                    append(getString(R.string.delete_team_question, nomEquip)).append("\n\n")
                    append(getString(R.string.will_delete_following)).append("\n")
                    append(getString(R.string.players_count, preview.jugadors.size)).append("\n")
                    append(getString(R.string.total_sessions_count, preview.sessionsCount)).append("\n\n")
                    append(getString(R.string.cannot_undo))
                }
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_delete)
                    .setMessage(message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        vm.delete(
                            idEquip = idEquip,
                            temporadaId = temporadaId,
                            onDone = {
                                Toast.makeText(requireContext(), getString(R.string.team_deleted), Toast.LENGTH_SHORT).show()
                            },
                            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .show()
            },
            onError = { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
