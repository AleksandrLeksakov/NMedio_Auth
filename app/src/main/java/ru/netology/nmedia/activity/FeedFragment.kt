package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.viewmodel.PostViewModel



class FeedFragment : Fragment() {
    // Переменная для хранения Snackbar (может быть null)
    private var snackbar: Snackbar? = null
    // Получаем ViewModel, которая будет жить пока жива Activity
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Создаем binding для этого фрагмента
        val binding = FragmentFeedBinding.inflate(inflater, container, false)

        // Создаем адаптер для RecyclerView с обработчиками взаимодействий
        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
                // Переходим к фрагменту редактирования
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            }

            override fun onImageClick(post: Post, attachment: Attachment) {
                val action = FeedFragmentDirections
                    .actionFeedFragmentToImageFragment(imageUrl = attachment.url)
                findNavController().navigate(action)
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onShare(post: Post) {
                // Создаем intent для шаринга
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                // Показываем диалог выбора приложения для шаринга
                val shareIntent = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(shareIntent)
            }
        })


        // Настраиваем RecyclerView
        binding.list.adapter = adapter
        binding.list.layoutManager = LinearLayoutManager(requireContext())

        // Наблюдаем за состоянием загрузки данных
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                // Показываем ошибку с возможностью повторить
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
        }

        // Наблюдаем за данными постов
        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts) {
                binding.emptyText.isVisible = state.empty
                // Прокручиваем вверх при обновлении
                binding.list.smoothScrollToPosition(0)
            }
        }

        // Наблюдаем за количеством скрытых постов
        viewModel.hiddenCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                showNewPostsNotification(binding, count)
            } else {
                snackbar?.dismiss()
            }
        }

        // Обработчик события прокрутки вверх
        viewModel.scrollToTopEvent.observe(viewLifecycleOwner) {
            binding.list.smoothScrollToPosition(0)
        }

        // Обработчик pull-to-refresh
        binding.swiperefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        // Обработчик клика по FAB
        binding.fab.setOnClickListener {
            viewModel.edit(createEmptyPost())
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return binding.root
    }

    // Создаем пустой пост для редактирования
    private fun createEmptyPost(): Post {
        return Post(
            id = 0,
            authorId = 0L,
            content = "",
            author = "",
            authorAvatar = "",
            likedByMe = false,
            likes = 0,
            published = "",
            isHidden = false
        )
    }

    // Показываем уведомление о новых постах
    private fun showNewPostsNotification(binding: FragmentFeedBinding, count: Int) {
        snackbar?.dismiss() // Закрываем предыдущий snackbar если был
        snackbar = Snackbar.make(
            binding.root,
            resources.getQuantityString(R.plurals.new_posts_notification, count, count),
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction(R.string.show) {
                viewModel.showAllHiddenPosts()
                dismiss()
            }
            anchorView = binding.fab // Привязываем к FAB чтобы не перекрывать его
            animationMode = Snackbar.ANIMATION_MODE_SLIDE
            show()
        }
    }

    override fun onDestroyView() {
        // Убираем snackbar при уничтожении View
        snackbar?.dismiss()
        super.onDestroyView()
    }
}