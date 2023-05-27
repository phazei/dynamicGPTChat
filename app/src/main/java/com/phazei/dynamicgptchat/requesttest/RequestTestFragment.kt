package com.phazei.dynamicgptchat.requesttest

import com.phazei.dynamicgptchat.R
import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.phazei.dynamicgptchat.data.datastore.Theme
import com.phazei.dynamicgptchat.databinding.FragmentRequestTestBinding
import com.phazei.utils.IdenticonFlorash
import com.phazei.utils.Solacon
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class RequestTestFragment : Fragment() {

    private var _binding: FragmentRequestTestBinding? = null
    private val binding get() = _binding!!
    private val testViewModel: RequestTestViewModel by activityViewModels()
    private var requestJob: Job? = null
    private val drawableLoadingStop: AnimatedVectorDrawable by lazy { ContextCompat.getDrawable(requireContext(),
        R.drawable.stop_and_load) as AnimatedVectorDrawable
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                testViewModel.requestUpdate
                    .filterNotNull()
                    .catch { e->
                        Log.d("TAG", "Request Exception {${e.message}}")
                    }
                    .collect { data ->
                        // binding.editResponse.setText(data as String)
                    }
            }
        }
        // enable scrolling in text views while in scroll view:
        // binding.editPrompt.setOnTouchListener { view, event ->
        //     view.parent.requestDisallowInterceptTouchEvent(true)
        //     if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
        //         view.parent.requestDisallowInterceptTouchEvent(false)
        //     }
        //     return@setOnTouchListener false
        // }
        // binding.editResponse.setOnTouchListener { view, event ->
        //     view.parent.requestDisallowInterceptTouchEvent(true)
        //     if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
        //         view.parent.requestDisallowInterceptTouchEvent(false)
        //     }
        //     return@setOnTouchListener false
        // }

        binding.toggleChat.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.toggleChat.text = "Using ChatCompletion"
            } else {
                binding.toggleChat.text = "Using Completion"
            }
        }

        binding.submitButton.setOnClickListener {
            if (requestJob?.isActive == true) {
                requestJob?.cancel()
                makeButtonReady()
            } else {
                makeTestRequest()
            }
        }

        setupModelsSpinner()

    }

    fun makeTestRequest() {
        requestJob = viewLifecycleOwner.lifecycleScope.launch() {
            makeButtonWait()
            binding.responseError.text = ""
            binding.editResponse.setText("")
            testViewModel.getConversationTitle(binding.editPrompt.text.toString())
            // testViewModel.getGeneralRequest(binding.editPrompt.text.toString(), binding.modelSpinner.selectedItem.toString(), binding.toggleChat.isChecked)
                .onCompletion {
                    makeButtonReady()
                    Snackbar.make(binding.root,"Request Complete", Snackbar.LENGTH_SHORT).show()
                }
                .catch { e ->
                    Log.d("TAG", "Cold Flow Error: {${e.message}}")
                    binding.responseError.text = e.message
                }
                .collect { data ->
                    binding.editResponse.setText(data as String)
                }
        }
    }

    fun makeButtonWait() {
        binding.submitButton.icon = drawableLoadingStop
        binding.submitButton.text = "Click to Stop"
        drawableLoadingStop.start()
    }
    fun makeButtonReady() {
        binding.submitButton.text = "Submit"
        binding.submitButton.icon = null
    }

    fun setupModelsSpinner() {
        viewLifecycleOwner.lifecycleScope.launch() {
            testViewModel.getModels().catch { e ->
                var message = e.message
                Snackbar.make(binding.root,"${e.message} Invalid OpenAI Token: Unable to retrieve model list.", Snackbar.LENGTH_LONG).show()
            }.collect { modelIds: List<String> ->
                val modelAdapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    modelIds
                )
                binding.modelSpinner.apply {
                    adapter = modelAdapter
                }
            }
        }
        binding.modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedModelId = parent?.getItemAtPosition(position).toString()
                binding.iconSolacon.setImageBitmap(Solacon.generateBitmap(selectedModelId, 256))
                binding.iconFlorash.setImageBitmap(IdenticonFlorash.generateBitmap(selectedModelId, 256))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
