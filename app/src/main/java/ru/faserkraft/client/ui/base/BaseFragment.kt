package ru.faserkraft.client.ui.base

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Base Fragment для всех фрагментов приложения
 * Обеспечивает общую логику работы с диалогами и ошибками
 */
abstract class BaseFragment<VM : ViewModel> : Fragment() {

    protected abstract val viewModel: VM

    protected var activeDialog: AlertDialog? = null

    /**
     * Показать простой диалог с сообщением
     */
    protected fun showDialog(message: String, onPositive: () -> Unit = {}) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ ->
                onPositive()
                dialog.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    /**
     * Показать диалог с подтверждением
     */
    protected fun showConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
                activeDialog = null
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                onCancel()
                dialog.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    /**
     * Показать диалог ошибки
     */
    protected fun showErrorDialog(error: Throwable) {
        showDialog(error.message ?: "Неизвестная ошибка")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
    }
}

