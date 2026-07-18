package com.personalmorningalarm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.personalmorningalarm.R
import com.personalmorningalarm.data.model.ShoppingListSummaryDto
import com.personalmorningalarm.data.remote.AlfredRepository
import com.personalmorningalarm.data.remote.AlfredResult
import com.personalmorningalarm.databinding.FragmentShoppingMenuBinding
import kotlinx.coroutines.launch

/**
 * The shopping-list tile's landing screen: every active list from Alfred's discovery
 * endpoint, plus creating a new one. Behaves like the other Alfred menus — cache-backed,
 * renders from the saved copy when Alfred is unreachable — except create-list, which
 * is deliberately online-only (it needs the API's 409 answer, so there's nothing a
 * queue could usefully do with it offline).
 */
class ShoppingMenuFragment : Fragment() {

    private var _binding: FragmentShoppingMenuBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShoppingMenuViewModel by viewModels {
        AlfredViewModelFactory(AlfredRepository(requireContext()))
    }

    private val listAdapter by lazy {
        ShoppingListSummaryAdapter(onClick = ::openList)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.rvShoppingLists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShoppingLists.adapter = listAdapter

        binding.btnShoppingNewList.setOnClickListener { submitCreate() }
        binding.etShoppingNewList.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitCreate()
                true
            } else {
                false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::showEvent) }
            }
        }
    }

    private fun submitCreate() {
        val name = binding.etShoppingNewList.text?.toString().orEmpty()
        if (name.isBlank()) return
        viewModel.createList(name)
    }

    private fun openList(summary: ShoppingListSummaryDto) {
        val id = summary.id ?: return
        findNavController().navigate(
            R.id.action_shoppingMenu_to_shoppingList,
            bundleOf(
                ShoppingListFragment.ARG_LIST_ID to id,
                ShoppingListFragment.ARG_LIST_TITLE to (summary.title ?: id)
            )
        )
    }

    private fun showEvent(event: ShoppingMenuViewModel.CreateEvent) {
        when (event) {
            is ShoppingMenuViewModel.CreateEvent.Created -> {
                binding.etShoppingNewList.setText("")
                Snackbar.make(
                    binding.root,
                    getString(R.string.shopping_new_list_created, event.title),
                    Snackbar.LENGTH_LONG
                ).show()
            }
            ShoppingMenuViewModel.CreateEvent.Conflict ->
                Snackbar.make(binding.root, R.string.shopping_new_list_conflict, Snackbar.LENGTH_LONG).show()
            ShoppingMenuViewModel.CreateEvent.Unreachable ->
                Snackbar.make(binding.root, R.string.shopping_new_list_needs_alfred, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun render(state: ShoppingMenuViewModel.MenuState) {
        binding.swipeRefresh.isRefreshing = state.loading

        binding.tvShoppingMenuNote.isVisible = false
        val lists: List<ShoppingListSummaryDto>? = when (val result = state.lists) {
            null -> null
            is AlfredResult.Fresh -> result.data
            is AlfredResult.Stale -> {
                binding.tvShoppingMenuNote.isVisible = true
                binding.tvShoppingMenuNote.text = getString(R.string.schedule_stale)
                result.data
            }
            AlfredResult.Unavailable -> null
        }

        // The new-list affordance needs a live Alfred; the hint says so whenever the
        // last discovery fetch wasn't fresh.
        binding.tvShoppingNewListHint.isVisible = state.lists !is AlfredResult.Fresh

        if (lists == null) {
            binding.tvShoppingMenuBody.isVisible = true
            binding.rvShoppingLists.isVisible = false
            binding.tvShoppingMenuBody.text = getString(
                if (state.lists == null) R.string.shopping_menu_loading else R.string.shopping_menu_unavailable
            )
            listAdapter.submitList(emptyList())
            return
        }

        binding.rvShoppingLists.isVisible = lists.isNotEmpty()
        binding.tvShoppingMenuBody.isVisible = lists.isEmpty()
        if (lists.isEmpty()) {
            binding.tvShoppingMenuBody.text = getString(R.string.shopping_menu_empty)
        }
        listAdapter.submitList(lists)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvShoppingLists.adapter = null
        _binding = null
    }
}
