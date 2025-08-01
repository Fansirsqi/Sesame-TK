package fansirsqi.xposed.sesame.ui.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.ExtendFunctionItem

class ExtendFunctionAdapter(
    private val items: List<ExtendFunctionItem>
) : RecyclerView.Adapter<ExtendFunctionAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_extend_function, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.button.text = item.name
        holder.button.setOnClickListener { item.action() }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.button_extend_item)
    }
}