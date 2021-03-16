/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.activities.main.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import org.linphone.LinphoneApplication
import org.linphone.R
import org.linphone.activities.main.viewmodels.DialogViewModel
import org.linphone.activities.main.viewmodels.ListTopBarViewModel
import org.linphone.core.tools.Log
import org.linphone.utils.AppUtils
import org.linphone.utils.DialogUtils
import org.linphone.utils.SelectionListAdapter

/**
 * This fragment can be inherited by all fragments that will display a list
 * where items can be selected for removal through the ListTopBarFragment
 */
abstract class MasterFragment<T : ViewDataBinding, U : SelectionListAdapter<*, *>> : SecureFragment<T>() {
    protected var _adapter: U? = null
    protected val adapter: U
        get() {
            if (_adapter == null) {
                Log.e("[Master Fragment] Attempting to get a null adapter!")
            }
            return _adapter!!
        }

    protected lateinit var listSelectionViewModel: ListTopBarViewModel
    protected open val dialogConfirmationMessageBeforeRemoval: Int = R.plurals.dialog_default_delete

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // List selection
        listSelectionViewModel = ViewModelProvider(this).get(ListTopBarViewModel::class.java)

        listSelectionViewModel.isEditionEnabled.observe(viewLifecycleOwner, {
            if (!it) listSelectionViewModel.onUnSelectAll()
        })

        listSelectionViewModel.selectAllEvent.observe(viewLifecycleOwner, {
            it.consume {
                listSelectionViewModel.onSelectAll(getItemCount() - 1)
            }
        })

        listSelectionViewModel.unSelectAllEvent.observe(viewLifecycleOwner, {
            it.consume {
                listSelectionViewModel.onUnSelectAll()
            }
        })

        listSelectionViewModel.deleteSelectionEvent.observe(viewLifecycleOwner, {
            it.consume {
                val confirmationDialog = AppUtils.getStringWithPlural(dialogConfirmationMessageBeforeRemoval, listSelectionViewModel.selectedItems.value.orEmpty().size)
                val viewModel = DialogViewModel(confirmationDialog)
                val dialog: Dialog = DialogUtils.getDialog(requireContext(), viewModel)

                viewModel.showCancelButton {
                    dialog.dismiss()
                    listSelectionViewModel.isEditionEnabled.value = false
                }

                viewModel.showDeleteButton({
                    delete()
                    dialog.dismiss()
                    listSelectionViewModel.isEditionEnabled.value = false
                }, getString(R.string.dialog_delete))

                dialog.show()
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Do not use postponeEnterTransition when fragment is recreated from the back stack,
        // otherwise the previous fragment will be visible until the animation starts
        if (LinphoneApplication.corePreferences.enableAnimations) {
            val resume =
                findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("resume")?.value
                    ?: false
            if (!resume) {
                findNavController().currentBackStackEntry?.savedStateHandle?.set("resume", true)
                // To ensure animation will be smooth,
                // wait until the adapter is loaded to display the fragment
                postponeEnterTransition()
                view.doOnPreDraw { startPostponedEnterTransition() }
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun delete() {
        val list = listSelectionViewModel.selectedItems.value ?: arrayListOf()
        deleteItems(list)
    }

    private fun getItemCount(): Int {
        return adapter.itemCount
    }

    abstract fun deleteItems(indexesOfItemToDelete: ArrayList<Int>)
}