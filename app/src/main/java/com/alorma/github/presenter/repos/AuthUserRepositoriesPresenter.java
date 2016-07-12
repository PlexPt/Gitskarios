package com.alorma.github.presenter.repos;

import android.support.annotation.NonNull;
import com.alorma.github.injector.PerActivity;
import com.alorma.github.sdk.core.datasource.CacheDataSource;
import com.alorma.github.sdk.core.datasource.CloudDataSource;
import com.alorma.github.sdk.core.datasource.RestWrapper;
import com.alorma.github.sdk.core.datasource.SdkItem;
import com.alorma.github.sdk.core.repositories.CloudUserRepositoriesDataSource;
import com.alorma.github.sdk.core.repositories.Repo;
import java.util.List;
import javax.inject.Inject;
import rx.Observable;

@PerActivity public class AuthUserRepositoriesPresenter extends RepositoriesPresenter {

  @Inject
  public AuthUserRepositoriesPresenter() {
    super();
  }

  @Override
  Observable<SdkItem<List<Repo>>> getFallbackApi() {
    return null;
  }

  @NonNull
  protected CacheDataSource<String, List<Repo>> getUserReposCacheDataSource() {
    return new AuthUserReposCache("auth_repos");
  }

  @NonNull
  protected CloudDataSource<String, List<Repo>> getCloudRepositoriesDataSource(
      RestWrapper reposRetrofit, String sortOrder) {
    return new CloudUserRepositoriesDataSource(reposRetrofit, sortOrder);
  }
}
