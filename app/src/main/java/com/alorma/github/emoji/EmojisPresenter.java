package com.alorma.github.emoji;

import com.alorma.github.sdk.core.datasource.CacheDataSource;
import com.alorma.github.sdk.core.datasource.CloudDataSource;
import com.alorma.github.sdk.core.datasource.SdkItem;
import com.alorma.github.sdk.core.repository.GenericRepository;
import com.alorma.github.sdk.core.usecase.GenericUseCase;
import java.util.List;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class EmojisPresenter {

  public EmojisPresenter() {
  }

  public Observable<List<Emoji>> getEmojis() {
    CacheDataSource<Void, List<Emoji>> cache = new EmojisCacheDataSource();
    CloudDataSource<Void, List<Emoji>> api = new EmojisApiDataSource(null);

    GenericRepository<Void, List<Emoji>> repository = new GenericRepository<>(cache, api);
    GenericUseCase<Void, List<Emoji>> useCase = new GenericUseCase<>(repository);

    return useCase.execute(new SdkItem<>(null))
        .map(SdkItem::getK)
        .filter(emojis -> emojis.size() > 0)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
  }

  public Observable<List<Emoji>> getEmojis(String filter) {
    Observable<List<Emoji>> emojis = getEmojis();
    if (filter != null) {
      return emojis.flatMap(Observable::from).filter(emoji -> emoji.getKey().contains(filter.toLowerCase())).toList();
    }
    return emojis;
  }
}
