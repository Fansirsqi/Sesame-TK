package fansirsqi.xposed.sesame.ui.widget;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.DiffUtil;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;

public class ContentPagerAdapter extends FragmentStateAdapter {
    private final List<ModelConfig> configs = new ArrayList<>();
    private static final String TAG = "ContentPagerAdapter";

    public ContentPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, Map<String, ModelConfig> configMap) {
        super(fragmentManager, lifecycle);
        if (configMap == null) {
            throw new IllegalArgumentException("ConfigMap cannot be null");
        }
        this.configs.addAll(configMap.values());
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(Map<String, ModelConfig> configMap) {
        if (configMap == null) {
            throw new IllegalArgumentException("ConfigMap cannot be null");
        }

        List<ModelConfig> newConfigs = new ArrayList<>(configMap.values());
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return configs.size();
            }

            @Override
            public int getNewListSize() {
                return newConfigs.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return configs.get(oldItemPosition).equals(newConfigs.get(newItemPosition));
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return configs.get(oldItemPosition).equals(newConfigs.get(newItemPosition));
            }
        });

        configs.clear();
        configs.addAll(newConfigs);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        try {
            if (position < 0 || position >= configs.size()) {
                throw new IndexOutOfBoundsException("Invalid position: " + position);
            }
            ModelConfig config = configs.get(position);
            ModelFields fields = config.getFields();
            if (fields == null) {
                throw new IllegalStateException("Fields cannot be null for config at position: " + position);
            }
            return new ContentFragment(new ArrayList<>(fields.values()));
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment at position: " + position, e);
            throw e;
        }
    }

    @Override
    public int getItemCount() {
        return configs.size();
    }

    public static class ContentFragment extends Fragment {
        private final ArrayList<ModelField<?>> modelFields;
        private RecyclerView recyclerView;

        public ContentFragment(ArrayList<ModelField<?>> modelFields) {
            this.modelFields = modelFields;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_settings_list, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            recyclerView = view.findViewById(R.id.rv_items);
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(new ContentAdapter(modelFields));
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            recyclerView = null;
        }

        public void scrollToTop() {
            if (recyclerView != null) {
                recyclerView.smoothScrollToPosition(0);
            }
        }
    }

    private static class ContentAdapter extends RecyclerView.Adapter<ContentAdapter.ViewHolder> {
        private final ArrayList<ModelField<?>> modelFields;

        public ContentAdapter(ArrayList<ModelField<?>> modelFields) {
            this.modelFields = modelFields;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_settings_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//            ViewGroup container = (ViewGroup) holder.itemView;
//            container.removeAllViews();
//            View fieldView = modelFields.get(position).getView(container.getContext());
//            if (fieldView != null) {
//                container.addView(fieldView);
//            }

            LinearLayout container = holder.itemView.findViewById(R.id.item_container);
            container.removeAllViews();
            View fieldView = modelFields.get(position).getView(container.getContext());
            if (fieldView != null) {
                container.addView(fieldView);
            }

        }

        @Override
        public int getItemCount() {
            return modelFields.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}