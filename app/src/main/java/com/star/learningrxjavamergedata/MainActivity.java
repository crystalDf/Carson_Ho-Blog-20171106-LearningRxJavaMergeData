package com.star.learningrxjavamergedata;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RxJava";

    private String mResult = "数据源来自 = " ;

    private static final String BASE_URL = "https://api.github.com";
    public static final String PATH = "/repos/{owner}/{repo}/contributors";

    private static final String OWNER = "square";
    private static final String REPO = "retrofit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Observable<String> network = Observable.just("网络");
        Observable<String> file = Observable.just("本地文件");

        Observable.merge(network, file)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d(TAG, "开始采用subscribe连接: merge");
                    }

                    @Override
                    public void onNext(String s) {
                        Log.d(TAG, "数据源有: " + s);
                        mResult += s + " ";
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d(TAG, "对Error事件作出响应");
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "对Complete事件作出响应");
                        Log.d(TAG, "获取数据完成");
                        Log.d(TAG, mResult);
                    }
                });

        // Create a very simple REST adapter which points the GitHub API.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        // Create an instance of our GitHub API interface.
        GitHub github = retrofit.create(GitHub.class);

        // Create an observable instance for looking up Retrofit contributors.
        Observable<List<Contributor>> contributorsObservable =
                github.contributors(OWNER, REPO);

        // Create an observable instance for looking up Retrofit htmlUrls.
        Observable<List<HtmlUrl>> htmlUrlsObservable =
                github.htmlUrls(OWNER, REPO);

        Observable
                .zip(contributorsObservable.subscribeOn(Schedulers.io()),
                        htmlUrlsObservable.subscribeOn(Schedulers.io()),
                        (contributors, htmlUrls) -> {

                            StringBuilder result = new StringBuilder();

                            for (Contributor contributor : contributors) {
                                result
                                        .append(contributor.login)
                                        .append(" (")
                                        .append(contributor.contributions)
                                        .append(")")
                                        .append("\n");
                            }

                            result
                                    .append(" & ")
                                    .append("\n");

                            for (HtmlUrl htmlUrl : htmlUrls) {
                                result
                                        .append(htmlUrl.id)
                                        .append(" (")
                                        .append(htmlUrl.html_url)
                                        .append(")")
                                        .append("\n");
                            }

                            return result.toString();
                        })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> Log.d(TAG, "最终接收到的数据是: " + s),
                        throwable -> Log.d(TAG, "失败"));
    }
}
