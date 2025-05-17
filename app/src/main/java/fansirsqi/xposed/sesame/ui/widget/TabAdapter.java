package fansirsqi.xposed.sesame.ui.widget;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import fansirsqi.xposed.sesame.R;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> {
    private final List<String> titles;
    private final OnTabClickListener listener;
    private int selectedPosition = 0;
    private final Context context;

    public TabAdapter(Context context, List<String> titles, OnTabClickListener listener) {
        this.context = context;
        this.titles = titles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(titles.get(position));
        holder.textView.setTextColor(ContextCompat.getColor(context, R.color.main_button_text));
        
        // 设置背景资源
        if (selectedPosition == position) {
//            holder.itemView.setBackgroundResource(R.drawable.tab_selected_background);
            holder.itemView.findViewById(R.id.indicator_bar).setBackgroundResource(R.color.item_selected_orange);
        } else {
//            holder.itemView.setBackgroundResource(R.drawable.tab_background);
            holder.itemView.findViewById(R.id.indicator_bar).setBackgroundResource(android.R.color.transparent);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (position != selectedPosition) {
                listener.onTabClick(position);
                setSelectedPosition(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    public void setSelectedPosition(int position) {
        int oldPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(oldPosition);
        notifyItemChanged(selectedPosition);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tab_text);
        }
    }

    public interface OnTabClickListener {
        void onTabClick(int position);
    }
}