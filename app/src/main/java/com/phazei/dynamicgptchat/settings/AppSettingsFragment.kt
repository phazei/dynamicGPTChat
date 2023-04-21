package com.phazei.dynamicgptchat.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.phazei.dynamicgptchat.SharedViewModel
import com.phazei.dynamicgptchat.data.datastore.AppSettings
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

        viewLifecycleOwner.lifecycleScope.launch {
            val appSettings = appSettingsViewModel.appSettingsFlow.first()
            binding.apiKeyText.setText(appSettings.openAIkey)
        }

        binding.apiKeyText.doOnTextChanged { text, _, _, _ ->
            saveApiKeyJob?.cancel()
            saveApiKeyJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(debounceDuration)
                appSettingsViewModel.updateAppSettings(AppSettings(openAIkey = text.toString()))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}