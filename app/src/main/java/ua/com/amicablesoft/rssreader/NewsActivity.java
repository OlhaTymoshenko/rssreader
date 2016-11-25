package ua.com.amicablesoft.rssreader;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NewsActivity extends AppCompatActivity implements NewsView {
    private final String TAG = NewsActivity.class.getSimpleName();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NewsPresenter presenter;
    private NewsAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        adapter = new NewsAdapter(getApplicationContext());
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        mSwipeRefreshLayout.setColorSchemeResources(R.color.refresh_color_1, R.color.refresh_color_2);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                presenter.forceReload();
            }
        });

        NewsRepository repository = new NewsRepository(getApplicationContext());
        presenter = new NewsPresenter(this, repository);
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.onStop();
    }

    @Override
    public void showNews(@NotNull List<News> newsFeed) {
        Log.i(TAG, "News: " + Arrays.toString(newsFeed.toArray()));
        adapter.setNewsList(newsFeed);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showError(@NotNull Throwable error) {
        Log.e(TAG, "Error", error);
        Toast.makeText(this, "Fail to load news", Toast.LENGTH_LONG).show();
        mSwipeRefreshLayout.setRefreshing(false);
    }


    public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<News> newsList = new ArrayList<>();
        private Context context;

        public NewsAdapter(Context context) {
            this.context = context;
        }

        public class NewsViewHolder extends RecyclerView.ViewHolder {
            final ImageView cardImage;
            final TextView cardTitle;
            final TextView cardDate;
            News news;

            public NewsViewHolder(View itemView) {
                super(itemView);
                cardImage = (ImageView) itemView.findViewById(R.id.card_image);
                cardTitle = (TextView) itemView.findViewById(R.id.card_title);
                cardDate = (TextView) itemView.findViewById(R.id.card_date);
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recycler_view, parent, false);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int itemPosition = recyclerView.getChildLayoutPosition(v);
                    News news = newsList.get(itemPosition);
                    String link = news.getLink();
                    Intent intent = new Intent(NewsActivity.this, WebActivity.class);
                    intent.putExtra("link", link);
                    startActivity(intent);
                }
            });
            return new NewsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            NewsViewHolder newsViewHolder = (NewsViewHolder) holder;
            News news = newsList.get(position);
            String image = news.getImage();
            Picasso.with(context).load(image).into(newsViewHolder.cardImage);
            newsViewHolder.cardTitle.setText(news.getTitle());
            newsViewHolder.cardDate.setText(news.getDate().toString());
            newsViewHolder.news = news;
        }

        @Override
        public int getItemCount() {
            return newsList.size();
        }

        public void setNewsList(List<News> newsList) {
            this.newsList.clear();
            this.newsList.addAll(newsList);
            notifyDataSetChanged();
        }
    }
}


