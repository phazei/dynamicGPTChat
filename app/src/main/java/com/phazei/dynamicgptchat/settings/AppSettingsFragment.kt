package com.phazei.dynamicgptchat.settings

import android.R
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.datastore.AppSettings
import com.phazei.dynamicgptchat.data.datastore.Theme
import com.phazei.dynamicgptchat.databinding.FragmentAppSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppSettingsFragment : Fragment() {

    private var _binding: FragmentAppSettingsBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()
    private val appSettingsViewModel: AppSettingsViewModel by viewModels()

    private var saveApiKeyJob: Job? = null
    private val debounceDuration = 300L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupThemeSpinner()

        viewLifecycleOwner.lifecycleScope.launch {
            val appSettings = appSettingsViewModel.appSettingsFlow.first()

            binding.apiKeyText.setText(appSettings.openAIkey)

            val savedThemeIndex = appSettings.theme.ordinal
            binding.themeSpinner.setSelection(savedThemeIndex)

            binding.apiKeyText.doOnTextChanged { text, _, _, _ ->
                saveApiKeyJob?.cancel()
                saveApiKeyJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(debounceDuration)
                    appSettings.openAIkey = text.toString()
                    appSettingsViewModel.updateAppSettings(appSettings.copy())
                }
            }

            binding.themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedTheme = Theme.values()[position]
                    if (appSettings.theme != selectedTheme) {
                        appSettings.theme = selectedTheme
                        appSettingsViewModel.updateAppSettings(appSettings.copy())
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }


        }

    }

    private fun setupThemeSpinner() {
        val themes = Theme.values()
        val themeNames = themes.map { it.name }.toTypedArray()

        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, themeNames)
        spinnerAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.themeSpinner.adapter = spinnerAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}