package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.AuthState



@AndroidEntryPoint
class SignInFragment : Fragment() {
    private lateinit var binding: FragmentSignInBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeAuthState()

        binding.loginEditText.setText("student")
        binding.passwordEditText.setText("secret")
    }

    private fun setupListeners() {
        binding.signInButton.setOnClickListener {
            val login = binding.loginEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (login.isNotEmpty() && password.isNotEmpty()) {
                viewModel.authenticate(login, password)
            } else {
                Snackbar.make(binding.root, R.string.fill_all_fields, Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.passwordEditText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                binding.signInButton.performClick()
                true
            } else {
                false
            }
        }
    }

    private fun observeAuthState() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> {  // УБРАТЬ AuthViewModel.
                    showLoading(true)
                }
                is AuthState.Success -> {  // УБРАТЬ AuthViewModel.
                    showLoading(false)
                    findNavController().popBackStack()
                    Snackbar.make(requireView(), R.string.authentication_success, Snackbar.LENGTH_SHORT).show()
                }
                is AuthState.Error -> {  // УБРАТЬ AuthViewModel.
                    showLoading(false)
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                is AuthState.Idle -> {  // УБРАТЬ AuthViewModel. и добавить этот case
                    showLoading(false)
                }
            }
        }
    }

    private fun showLoading(loading: Boolean) {
        binding.signInButton.isEnabled = !loading
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }
}