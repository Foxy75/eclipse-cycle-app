package io.github.foxy75.eclipse.ui.settings

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.foxy75.eclipse.MainActivity
import io.github.foxy75.eclipse.R
import io.github.foxy75.eclipse.data.Repository
import io.github.foxy75.eclipse.databinding.FragmentSettingsBinding
import io.github.foxy75.eclipse.ui.common.Press
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val createDoc =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri ?: return@registerForActivityResult
            showPassphraseDialog(getString(R.string.backup_pass_title_export), needConfirm = true) { passphrase ->
                exportTo(uri, passphrase)
            }
        }

    private val openDoc =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@registerForActivityResult
            val bytes = readBytes(uri)
            if (bytes == null) {
                toast(getString(R.string.backup_import_fail))
                return@registerForActivityResult
            }
            showPassphraseDialog(getString(R.string.backup_pass_title_import), needConfirm = false) { passphrase ->
                importFrom(bytes, passphrase)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val repo = Repository.get(requireContext())
        Press.attach(binding.btnExport, binding.btnImport, binding.btnLockNow)

        binding.secureSwitch.isChecked = repo.secureFlag()
        binding.secureSwitch.setOnCheckedChangeListener { _, checked ->
            repo.setSecureFlag(checked)
            (activity as? MainActivity)?.applySecureFlag()
        }

        setupLanguage()
        binding.langGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) applyLanguage(checkedIds.first())
        }

        binding.btnExport.setOnClickListener { createDoc.launch("Eclipse-backup.ecl") }
        binding.btnImport.setOnClickListener { openDoc.launch(arrayOf("application/octet-stream", "*/*")) }
        binding.btnLockNow.setOnClickListener { (activity as? MainActivity)?.lockNow() }

        val versionName = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (_: Exception) {
            "1.0"
        }
        binding.tvVersion.text = getString(R.string.set_version, versionName)
    }

    private fun setupLanguage() {
        val locales = AppCompatDelegate.getApplicationLocales()
        val tag = if (locales.isEmpty) "" else locales.get(0)?.toLanguageTag() ?: ""
        binding.chipLangSystem.isChecked = tag.isEmpty()
        binding.chipLangId.isChecked = tag.startsWith("id")
        binding.chipLangEn.isChecked = tag.startsWith("en")
    }

    private fun applyLanguage(id: Int) {
        val locales = when (id) {
            R.id.chipLangId -> LocaleListCompat.forLanguageTags("id")
            R.id.chipLangEn -> LocaleListCompat.forLanguageTags("en")
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
        requireActivity().recreate()
    }

    private fun exportTo(uri: Uri, passphrase: String) {
        val repo = Repository.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = repo.export(passphrase)
                    writeBytes(uri, bytes)
                    true
                }.getOrDefault(false)
            }
            toast(
                if (ok) getString(R.string.backup_export_done, "Downloads")
                else getString(R.string.backup_export_fail)
            )
        }
    }

    private fun importFrom(bytes: ByteArray, passphrase: String) {
        val repo = Repository.get(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.import(bytes, passphrase) }
            toast(
                if (ok) getString(R.string.backup_import_done)
                else getString(R.string.backup_import_fail)
            )
        }
    }

    private fun showPassphraseDialog(
        title: String, needConfirm: Boolean, onOk: (String) -> Unit
    ) {
        val container = LinearLayout(requireContext())
        val pad = (16 * resources.displayMetrics.density).toInt()
        container.setPadding(pad, pad / 2, pad, 0)
        container.orientation = LinearLayout.VERTICAL

        val first = EditText(requireContext()).apply {
            hint = getString(R.string.backup_pass_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(first)

        val second: EditText? = if (needConfirm) {
            EditText(requireContext()).apply {
                hint = getString(R.string.backup_pass_confirm_hint)
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        } else null
        second?.let { container.addView(it) }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pass = first.text.toString()
                if (pass.length < 8) {
                    toast(getString(R.string.backup_pass_short))
                    return@setPositiveButton
                }
                if (second != null && second.text.toString() != pass) {
                    toast(getString(R.string.backup_pass_mismatch))
                    return@setPositiveButton
                }
                onOk(pass)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        requireContext().contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
    }

    private fun readBytes(uri: Uri): ByteArray? = try {
        requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}