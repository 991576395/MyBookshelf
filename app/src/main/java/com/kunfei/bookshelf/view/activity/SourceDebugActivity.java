package com.kunfei.bookshelf.view.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.kunfei.basemvplib.impl.IPresenter;
import com.kunfei.bookshelf.R;
import com.kunfei.bookshelf.base.MBaseActivity;
import com.kunfei.bookshelf.bean.BookContentBean;
import com.kunfei.bookshelf.bean.BookShelfBean;
import com.kunfei.bookshelf.bean.ChapterListBean;
import com.kunfei.bookshelf.bean.SearchBookBean;
import com.kunfei.bookshelf.help.BookshelfHelp;
import com.kunfei.bookshelf.model.WebBookModel;
import com.kunfei.bookshelf.utils.RxUtils;
import com.kunfei.bookshelf.utils.SoftInputUtil;
import com.kunfei.bookshelf.utils.TimeUtils;
import com.kunfei.bookshelf.utils.theme.ThemeStore;
import com.victor.loading.rotate.RotateLoading;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class SourceDebugActivity extends MBaseActivity {
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.searchView)
    SearchView searchView;
    @BindView(R.id.loading)
    RotateLoading loading;
    @BindView(R.id.action_bar)
    AppBarLayout actionBar;
    @BindView(R.id.tv_content)
    TextView tvContent;

    private String sourceUrl;
    private CompositeDisposable compositeDisposable;
    private DateFormat dateFormat = new SimpleDateFormat("mm:ss.SSS", Locale.getDefault());

    public static void startThis(Context context, String sourceUrl) {
        if (TextUtils.isEmpty(sourceUrl)) return;
        Intent intent = new Intent(context, SourceDebugActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("sourceUrl", sourceUrl);
        context.startActivity(intent);
    }

    /**
     * P层绑定   若无则返回null;
     */
    @Override
    protected IPresenter initInjector() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
    }

    /**
     * 布局载入  setContentView()
     */
    @Override
    protected void onCreateActivity() {
        getWindow().getDecorView().setBackgroundColor(ThemeStore.backgroundColor(this));
        setContentView(R.layout.activity_source_debug);
        ButterKnife.bind(this);
        this.setSupportActionBar(toolbar);
        setupActionBar();
    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 数据初始化
     */
    @Override
    protected void initData() {
        sourceUrl = getIntent().getStringExtra("sourceUrl");
        initSearchView();
    }

    private void initSearchView() {
        searchView.setQueryHint(getString(R.string.search_book_key));
        searchView.onActionViewExpanded();
        searchView.setSubmitButtonEnabled(true);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (TextUtils.isEmpty(query))
                    return false;
                startDebug(query);
                SoftInputUtil.hideIMM(searchView);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void startDebug(String key) {
        if (TextUtils.isEmpty(sourceUrl)) return;
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
        }
        compositeDisposable = new CompositeDisposable();
        loading.start();
        WebBookModel.getInstance().searchBook(key, 1, sourceUrl)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<List<SearchBookBean>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onNext(List<SearchBookBean> searchBookBeans) {
                        tvContent.setText(getString(R.string.get_book_list_success, searchBookBeans.size()));
                        SearchBookBean searchBookBean = searchBookBeans.get(0);
                        tvContent.setText(String.format("%s\n%s 书名:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getName()));
                        tvContent.setText(String.format("%s\n%s 作者:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getAuthor()));
                        tvContent.setText(String.format("%s\n%s 分类:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getKind()));
                        tvContent.setText(String.format("%s\n%s 简介:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getOrigin()));
                        tvContent.setText(String.format("%s\n%s 封面地址:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getCoverUrl()));
                        tvContent.setText(String.format("%s\n%s 最新章节:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getLastChapter()));
                        tvContent.setText(String.format("%s\n%s 书籍地址:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), searchBookBean.getNoteUrl()));
                        if (!TextUtils.isEmpty(searchBookBean.getNoteUrl())) {
                            bookInfoDebug(BookshelfHelp.getBookFromSearchBook(searchBookBean));
                        } else {
                            loading.stop();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(e.getMessage());
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private void bookInfoDebug(BookShelfBean bookShelfBean) {
        WebBookModel.getInstance().getBookInfo(bookShelfBean)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<BookShelfBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(BookShelfBean bookShelfBean) {
                        tvContent.setText(String.format("%s\n%s最新章节:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), bookShelfBean.getLastChapterName()));
                        tvContent.setText(String.format("%s\n%s封面:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), bookShelfBean.getBookInfoBean().getCoverUrl()));
                        tvContent.setText(String.format("%s\n%s简介:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), bookShelfBean.getBookInfoBean().getIntroduce()));
                        tvContent.setText(String.format("%s\n%s目录地址:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), bookShelfBean.getBookInfoBean().getChapterUrl()));
                        bookChapterListDebug(bookShelfBean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n%s加载书籍信息错误:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void bookChapterListDebug(BookShelfBean bookShelfBean) {
        WebBookModel.getInstance().getChapterList(bookShelfBean)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<BookShelfBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @SuppressLint("DefaultLocale")
                    @Override
                    public void onNext(BookShelfBean bookShelfBean) {
                        tvContent.setText(String.format("%s\n%s 获取目录数量:%d", tvContent.getText(), TimeUtils.getNowString(dateFormat), bookShelfBean.getChapterList().size()));
                        if (bookShelfBean.getChapterList().size() > 0) {
                            ChapterListBean chapterListBean = bookShelfBean.getChapter(0);
                            tvContent.setText(String.format("%s\n%s 章节名称:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), chapterListBean.getDurChapterName()));
                            tvContent.setText(String.format("%s\n%s 章节地址:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), chapterListBean.getDurChapterUrl()));
                            if (!TextUtils.isEmpty(chapterListBean.getDurChapterUrl())) {
                                bookContentDebug(chapterListBean, bookShelfBean.getBookInfoBean().getName());
                            } else {
                                loading.stop();
                            }
                        } else {
                            loading.stop();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n%s 加载目录错误:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void bookContentDebug(ChapterListBean chapterListBean, String bookName) {
        WebBookModel.getInstance().getBookContent(chapterListBean, bookName)
                .compose(RxUtils::toSimpleSingle)
                .subscribe(new Observer<BookContentBean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(BookContentBean bookContentBean) {
                        tvContent.setText(String.format("%s\n%s 正文:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), bookContentBean.getDurChapterContent()));
                    }

                    @Override
                    public void onError(Throwable e) {
                        tvContent.setText(String.format("%s\n%s 加载正文错误:%s", tvContent.getText(), TimeUtils.getNowString(dateFormat), e.getMessage()));
                        loading.stop();
                    }

                    @Override
                    public void onComplete() {
                        loading.stop();
                    }
                });
    }
}
