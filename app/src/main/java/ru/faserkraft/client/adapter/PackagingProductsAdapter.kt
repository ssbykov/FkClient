package ru.faserkraft.client.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.databinding.ItemPackagingProductBinding

class PackagingProductsAdapter(
    private val onItemCheckedChange: (item: PackagingProductUiItem, isChecked: Boolean) -> Unit
) : ListAdapter<PackagingProductUiItem, PackagingProductsAdapter.ProductVH>(ProductDiff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductVH {
        val binding = ItemPackagingProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductVH(binding, onItemCheckedChange)
    }

    override fun onBindViewHolder(holder: ProductVH, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductVH(
        private val binding: ItemPackagingProductBinding,
        private val onItemCheckedChange: (item: PackagingProductUiItem, isChecked: Boolean) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackagingProductUiItem) = with(binding) {
            // номер продукта
            tvProductSerial.text = item.serialNumber
            // название процесса
            tvProcessName.text = item.processName

            cbSelected.setOnCheckedChangeListener(null)
            cbSelected.isChecked = item.isSelected

            cbSelected.setOnCheckedChangeListener { _, isChecked ->
                onItemCheckedChange(item, isChecked)
            }

            // по клику по всей карточке тоже переключаем чекбокс
            root.setOnClickListener {
                cbSelected.isChecked = !cbSelected.isChecked
            }
        }
    }

    class ProductDiff : DiffUtil.ItemCallback<PackagingProductUiItem>() {
        override fun areItemsTheSame(
            oldItem: PackagingProductUiItem,
            newItem: PackagingProductUiItem
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PackagingProductUiItem,
            newItem: PackagingProductUiItem
        ): Boolean = oldItem == newItem
    }
}

data class PackagingProductUiItem(
    val id: Int,
    val serialNumber: String,
    val processName: String,
    val isSelected: Boolean = false,
)
