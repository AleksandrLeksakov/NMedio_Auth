
package ru.netology.nmedia.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.databinding.FragmentImageBinding
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ImageFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentImageBinding.inflate(inflater, container, false).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentImageBinding.bind(view)
        val imageUrl = arguments?.getString("imageUrl") ?: return

        Glide.with(requireContext())
            .load("${BuildConfig.BASE_URL}/media/$imageUrl")
            .into(binding.fullscreenImage)

        binding.fullscreenImage.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.closeButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}