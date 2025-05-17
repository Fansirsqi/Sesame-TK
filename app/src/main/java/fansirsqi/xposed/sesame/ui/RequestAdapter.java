package fansirsqi.xposed.sesame.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fansirsqi.xposed.sesame.R;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {
    private final List<RequestItem> requestList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RequestItem item, String action);
    }

    public RequestAdapter(List<RequestItem> requestList, OnItemClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_item, parent, false);
        return new ViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestItem item = requestList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        EditText method, data;
        Button sendButton, testButton, deleteButton;

        private RequestItem item;
        private final OnItemClickListener listener;

        public ViewHolder(@NonNull View itemView, OnItemClickListener listener) {
            super(itemView);
            this.listener = listener;

            title = itemView.findViewById(R.id.item_title);
            method = itemView.findViewById(R.id.item_method);
            data = itemView.findViewById(R.id.item_data);
            sendButton = itemView.findViewById(R.id.send_button);
            testButton = itemView.findViewById(R.id.test_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(RequestItem item) {
            this.item = item;
            title.setText(item.getTitle());
            method.setText(item.getMethod());
            data.setText(item.getData());

            // 文本变化监听
            method.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    item.setMethod(s.toString());
                }
            });

            data.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    item.setData(s.toString());
                }
            });

            // 按钮点击监听
            sendButton.setOnClickListener(v -> {
                if (item != null && listener != null) {
                    listener.onItemClick(item, "send");
                }
            });

            testButton.setOnClickListener(v -> {
                if (item != null && listener != null) {
                    listener.onItemClick(item, "test");
                }
            });

            deleteButton.setOnClickListener(v -> {
                if (item != null && listener != null) {
                    listener.onItemClick(item, "delete");
                }
            });
        }
    }
}