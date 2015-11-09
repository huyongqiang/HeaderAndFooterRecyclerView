package com.cundong.recyclerview.sample;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cundong.recyclerview.EndlessRecyclerOnScrollListener;
import com.cundong.recyclerview.ExStaggeredGridLayoutManager;
import com.cundong.recyclerview.HeaderAndFooterRecyclerViewAdapter;
import com.cundong.recyclerview.HeaderSpanSizeLookup;
import com.cundong.recyclerview.RecyclerViewUtils;
import com.cundong.recyclerview.sample.utils.RecyclerViewStateUtils;
import com.cundong.recyclerview.sample.weight.LoadingFooter;
import com.cundong.recyclerview.sample.weight.SampleHeader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by cundong on 2015/10/29.
 *
 * 带HeaderView的分页加载GridLayout RecyclerView
 */
public class EndlessStaggeredGridLayoutActivity extends AppCompatActivity {

    /**服务器端一共多少条数据*/
    private static final int TOTAL_COUNTER = 64;

    /**每一页展示多少条数据*/
    private static final int REQUEST_COUNT = 10;

    /**已经获取到多少条数据了*/
    private int mCurrentCounter = 0;

    private RecyclerView mRecyclerView = null;

    private DataAdapter mDataAdapter = null;
    private ArrayList<String> mDataList = null;

    private PreviewHandler mHandler = new PreviewHandler(this);
    private HeaderAndFooterRecyclerViewAdapter mHeaderAndFooterRecyclerViewAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_activity);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);

        //init data
        mDataList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mDataList.add("item" + i);
        }

        mCurrentCounter = mDataList.size();

        mDataAdapter = new DataAdapter(this);
        mDataAdapter.setData(mDataList);

        mHeaderAndFooterRecyclerViewAdapter = new HeaderAndFooterRecyclerViewAdapter(mDataAdapter);
        mRecyclerView.setAdapter(mHeaderAndFooterRecyclerViewAdapter);

        //setLayoutManager
        ExStaggeredGridLayoutManager manager = new ExStaggeredGridLayoutManager (2, StaggeredGridLayoutManager.VERTICAL);
        manager.setSpanSizeLookup(new HeaderSpanSizeLookup((HeaderAndFooterRecyclerViewAdapter) mRecyclerView.getAdapter(), manager.getSpanCount()));
        mRecyclerView.setLayoutManager(manager);

        RecyclerViewUtils.setHeaderView(mRecyclerView, new SampleHeader(this));

        mRecyclerView.addOnScrollListener(mOnScrollListener);
    }

    private void notifyDataSetChanged() {
        mHeaderAndFooterRecyclerViewAdapter.notifyDataSetChanged();
    }

    private void refreshData() {
        mDataAdapter.setData(mDataList);
    }

    private EndlessRecyclerOnScrollListener mOnScrollListener = new EndlessRecyclerOnScrollListener() {

        @Override
        public void onLoadNextPage(View view) {
            super.onLoadNextPage(view);

            LoadingFooter.State state = RecyclerViewStateUtils.getFooterViewState(mRecyclerView);
            if(state == LoadingFooter.State.Loading) {
                Log.d("@Cundong", "the state is Loading, just wait..");
                return;
            }

            mCurrentCounter = mDataList.size();

            if (mCurrentCounter < TOTAL_COUNTER) {
                // loading more
                RecyclerViewStateUtils.setFooterViewState(EndlessStaggeredGridLayoutActivity.this, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.Loading, null);
                requestData();
            } else {
                //the end
                RecyclerViewStateUtils.setFooterViewState(EndlessStaggeredGridLayoutActivity.this, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.TheEnd, null);
            }
        }
    };

    private static class PreviewHandler extends Handler {

        private WeakReference<EndlessStaggeredGridLayoutActivity> ref;

        PreviewHandler(EndlessStaggeredGridLayoutActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final EndlessStaggeredGridLayoutActivity activity = ref.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            switch (msg.what) {
                case -1:
                    int currentSize = activity.mDataList.size();

                    //模拟组装数据
                    for (int i = 0; i < 10; i++) {
                        if(activity.mDataList.size() >= TOTAL_COUNTER) {
                            break;
                        }
                        activity.mDataList.add("item" + (currentSize+i));
                    }

                    activity.refreshData();
                    RecyclerViewStateUtils.setFooterViewState(activity.mRecyclerView, LoadingFooter.State.Normal);
                    break;
                case -2:
                    activity.notifyDataSetChanged();
                    break;
            }
        }
    }

    /**
     * 模拟请求网络
     */
    private void requestData() {

        new Thread() {

            @Override
            public void run() {
                super.run();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mHandler.sendEmptyMessage(-1);
            }
        }.start();
    }

    private class DataAdapter extends RecyclerView.Adapter {

        private LayoutInflater mLayoutInflater;
        private ArrayList<String> mDataList = new ArrayList<>();

        private int largeCardHeight, smallCardHeight;

        public DataAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
            largeCardHeight = (int)context.getResources().getDisplayMetrics().density * 300;
            smallCardHeight = (int)context.getResources().getDisplayMetrics().density * 200;
        }

        public void setData(ArrayList<String> list) {
            this.mDataList = list;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mLayoutInflater.inflate(R.layout.sample_item_card, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            String item = mDataList.get(position);

            ViewHolder viewHolder = (ViewHolder) holder;
            viewHolder.textView.setText(item);

            //修改高度，模拟交错效果
            viewHolder.cardView.getLayoutParams().height = position % 2 != 0 ? largeCardHeight : smallCardHeight;
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            private CardView cardView;
            private TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);
                cardView = (CardView) itemView.findViewById(R.id.card_view);
                textView = (TextView) itemView.findViewById(R.id.info_text);

                textView.setOnClickListener( new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text = mDataList.get(RecyclerViewUtils.getAdapterPosition(mRecyclerView, ViewHolder.this));
                        Toast.makeText(EndlessStaggeredGridLayoutActivity.this, text, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}